import { useState, useCallback } from 'react';

export interface ApiLogEntry {
  timestamp: string;
  request: any;
  response?: string;
  error?: string;
}

export function useApiLogs() {
  const [logs, setLogs] = useState<ApiLogEntry[]>([]);
  const [showLogs, setShowLogs] = useState(false);

  // Add a new log entry
  const addLog = useCallback((logEntry: {
    request: any;
    response?: string;
    error?: string;
  }) => {
    setLogs(prevLogs => [
      ...prevLogs,
      {
        ...logEntry,
        timestamp: new Date().toLocaleString()
      }
    ]);
  }, []);

  // Clear all logs
  const clearLogs = useCallback(() => {
    setLogs([]);
  }, []);

  // Toggle log visibility
  const toggleLogs = useCallback(() => {
    setShowLogs(prev => !prev);
  }, []);

  return {
    logs,
    showLogs,
    addLog,
    clearLogs,
    toggleLogs
  };
}