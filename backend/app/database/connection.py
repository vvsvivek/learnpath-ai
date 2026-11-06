"""
Database Connection Singleton Module for Supabase.
"""

import logging
from supabase import AsyncClient, acreate_client
from app.core.config import settings

logger = logging.getLogger(__name__)

# Global singleton client instance
supabase_client: AsyncClient | None = None


async def init_supabase() -> AsyncClient:
    """Initialize the singleton Supabase asynchronous client during application lifespan.

    Returns:
        The initialized AsyncClient instance.
    """
    global supabase_client
    if supabase_client is None:
        url = settings.SUPABASE_URL
        # Prefer service role key for full backend access, fallback to anon key
        key = settings.SUPABASE_SERVICE_ROLE_KEY or settings.SUPABASE_ANON_KEY
        
        if not url or not key:
            logger.warning(
                "SUPABASE_URL or SUPABASE keys are missing in environment! "
                "Supabase operations may fail until valid credentials are provided."
            )
            # Create client with placeholder to avoid immediate crash on startup if testing
            url = url or "https://placeholder.supabase.co"
            key = key or "placeholder-key"

        try:
            supabase_client = await acreate_client(url, key)
            logger.info("Supabase AsyncClient singleton initialized successfully.")
        except Exception as exc:
            logger.exception("Failed to initialize Supabase AsyncClient: %s", exc)
            raise exc

    return supabase_client


def get_client() -> AsyncClient:
    """Retrieve the active Supabase AsyncClient singleton.

    Returns:
        The active AsyncClient instance.

    Raises:
        RuntimeError: If the client has not been initialized via lifespan.
    """
    if supabase_client is None:
        raise RuntimeError(
            "Supabase client is not initialized. Ensure application lifespan startup has completed."
        )
    return supabase_client
