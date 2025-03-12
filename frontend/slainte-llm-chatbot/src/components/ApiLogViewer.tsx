import React from 'react';
import { ApiLogEntry } from '@/hooks/useApiLogs';

interface ApiLogViewerProps {
  logs: ApiLogEntry[];
  onClose: () => void;
}

const ApiLogViewer: React.FC<ApiLogViewerProps> = ({ logs, onClose }) => {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[10000] p-4">
      <div className="bg-white rounded-lg w-full max-w-4xl max-h-[90vh] flex flex-col">
        <div className="p-4 bg-gray-100 flex justify-between items-center border-b">
          <h2 className="text-lg font-bold">API Logs</h2>
          <div className="flex space-x-2">
            <button
              onClick={() => {
                // Copy logs to clipboard as JSON
                navigator.clipboard.writeText(JSON.stringify(logs, null, 2))
                  .then(() => alert('Logs copied to clipboard'))
                  .catch(err => console.error('Failed to copy logs:', err));
              }}
              className="px-3 py-1 bg-blue-500 text-white rounded text-sm hover:bg-blue-600"
            >
              Copy All
            </button>
            <button
              onClick={onClose}
              className="p-1 hover:bg-gray-200 rounded"
            >
              ✖
            </button>
          </div>
        </div>
        <div className="flex-1 overflow-auto p-4">
          {logs.length === 0 ? (
            <p className="text-gray-500">No logs yet</p>
          ) : (
            logs.map((log, index) => (
              <div key={index} className="mb-6 border-b pb-4">
                <div className="text-sm text-gray-500 mb-2">{log.timestamp}</div>
                <div className="mb-4">
                  <h3 className="font-semibold mb-2">Request Payload:</h3>
                  <pre className="bg-gray-100 p-3 rounded text-xs overflow-auto max-h-60">
                    {JSON.stringify(log.request, null, 2)}
                  </pre>
                </div>
                {log.response && (
                  <div className="mb-2">
                    <h3 className="font-semibold mb-2">Response:</h3>
                    <pre className="bg-gray-100 p-3 rounded text-xs overflow-auto max-h-60">
                      {log.response}
                    </pre>
                  </div>
                )}
                {log.error && (
                  <div className="mb-2 text-red-500">
                    <h3 className="font-semibold mb-2">Error:</h3>
                    <pre className="bg-red-50 p-3 rounded text-xs overflow-auto max-h-60">
                      {log.error}
                    </pre>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default ApiLogViewer;