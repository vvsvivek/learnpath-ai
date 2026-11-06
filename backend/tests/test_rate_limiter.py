import pytest
import asyncio
import time
from app.core.rate_limiter import InMemoryRateLimiter

@pytest.mark.anyio
async def test_rate_limiter_allows_under_limit():
    limiter = InMemoryRateLimiter()
    key = "test_key_1"
    for _ in range(5):
        allowed = await limiter.rate_limit(key, max_requests=5, window_seconds=60)
        assert allowed is True

@pytest.mark.anyio
async def test_rate_limiter_blocks_over_limit():
    limiter = InMemoryRateLimiter()
    key = "test_key_2"
    for _ in range(3):
        allowed = await limiter.rate_limit(key, max_requests=3, window_seconds=60)
        assert allowed is True
    
    # 4th request should be blocked
    allowed = await limiter.rate_limit(key, max_requests=3, window_seconds=60)
    assert allowed is False

@pytest.mark.anyio
async def test_rate_limiter_resets_after_window():
    limiter = InMemoryRateLimiter()
    key = "test_key_3"
    assert await limiter.rate_limit(key, max_requests=1, window_seconds=1) is True
    assert await limiter.rate_limit(key, max_requests=1, window_seconds=1) is False
    time.sleep(1.1)
    assert await limiter.rate_limit(key, max_requests=1, window_seconds=1) is True
