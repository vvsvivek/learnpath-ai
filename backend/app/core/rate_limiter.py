"""
In-Memory Rate Limiting Module.

Designed for single-instance / free-tier deployments.
Tracks request timestamps per key and prunes expired records automatically.
"""

import time
import asyncio
from typing import Callable
from fastapi import HTTPException, Request, status


class InMemoryRateLimiter:
    """Thread-safe and async-safe in-memory sliding window rate limiter."""

    def __init__(self) -> None:
        # Maps key -> list of float epoch timestamps
        self._store: dict[str, list[float]] = {}
        self._lock = asyncio.Lock()
        self._last_cleanup = time.time()

    async def rate_limit(self, key: str, max_requests: int, window_seconds: int) -> bool:
        """Check if request for key is within rate limits.

        Args:
            key: Unique identifier (e.g., client IP or email address).
            max_requests: Maximum allowed requests in the window.
            window_seconds: Window duration in seconds.

        Returns:
            True if allowed, False if limit exceeded.
        """
        async with self._lock:
            now = time.time()
            cutoff = now - window_seconds

            # Lazy periodic cleanup every 60 seconds
            if now - self._last_cleanup > 60:
                self._cleanup(now)
                self._last_cleanup = now

            timestamps = self._store.get(key, [])
            # Keep only timestamps within window
            valid_timestamps = [ts for ts in timestamps if ts > cutoff]

            if len(valid_timestamps) >= max_requests:
                self._store[key] = valid_timestamps
                return False

            valid_timestamps.append(now)
            self._store[key] = valid_timestamps
            return True

    def _cleanup(self, now: float) -> None:
        """Remove keys with empty or completely expired timestamps."""
        keys_to_delete = []
        for key, timestamps in self._store.items():
            valid = [ts for ts in timestamps if now - ts < 3600]
            if not valid:
                keys_to_delete.append(key)
            else:
                self._store[key] = valid

        for k in keys_to_delete:
            del self._store[k]


# Global rate limiter instance
limiter = InMemoryRateLimiter()


def rate_limit_dependency(
    max_requests: int = 5,
    window_seconds: int = 60,
    key_prefix: str = "general",
) -> Callable:
    """FastAPI dependency to rate limit endpoints based on client IP.

    Args:
        max_requests: Maximum requests allowed.
        window_seconds: Time window in seconds.
        key_prefix: Prefix identifier for the endpoint group.
    """
    async def dependency(request: Request) -> None:
        client_ip = request.client.host if request.client else "unknown"
        key = f"{key_prefix}:{client_ip}"
        allowed = await limiter.rate_limit(key, max_requests, window_seconds)
        if not allowed:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail=f"Rate limit exceeded. Maximum {max_requests} requests per {window_seconds} seconds.",
            )

    return dependency
