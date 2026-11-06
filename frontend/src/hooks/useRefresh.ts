import { useState, useCallback } from 'react';

export const useRefresh = (refreshFn: () => Promise<any>) => {
  const [refreshing, setRefreshing] = useState(false);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await refreshFn();
    } catch (error) {
      console.warn('Error during pull-to-refresh:', error);
    } finally {
      setRefreshing(false);
    }
  }, [refreshFn]);

  return {
    refreshing,
    onRefresh,
  };
};
