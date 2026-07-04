import React, { useState } from 'react';
import { useStorageStats, useAgentTraces, useAgentMemory } from '../hooks/useStorage';
import '../styles/storage-dashboard.css';

export function StorageDashboard() {
  const { stats, loading: statsLoading, error: statsError } = useStorageStats();
  const [activeTab, setActiveTab] = useState<'stats' | 'traces' | 'memory'>('stats');
  const [selectedAgent, setSelectedAgent] = useState('coordinator');

  return (
    <div className="storage-dashboard">
      <div className="storage-header">
        <h2>📊 Database Storage Dashboard</h2>
        <div className="storage-tabs">
          <button
            className={`tab ${activeTab === 'stats' ? 'active' : ''}`}
            onClick={() => setActiveTab('stats')}
          >
            Statistics
          </button>
          <button
            className={`tab ${activeTab === 'traces' ? 'active' : ''}`}
            onClick={() => setActiveTab('traces')}
          >
            Traces
          </button>
          <button
            className={`tab ${activeTab === 'memory' ? 'active' : ''}`}
            onClick={() => setActiveTab('memory')}
          >
            Memory
          </button>
        </div>
      </div>

      {statsError && (
        <div className="storage-error">
          ⚠️ {statsError}
        </div>
      )}

      {activeTab === 'stats' && (
        <StorageStats stats={stats} loading={statsLoading} />
      )}

      {activeTab === 'traces' && (
        <AgentTraces selectedAgent={selectedAgent} onAgentChange={setSelectedAgent} />
      )}

      {activeTab === 'memory' && (
        <AgentMemoryView selectedAgent={selectedAgent} onAgentChange={setSelectedAgent} />
      )}
    </div>
  );
}

interface StorageStatsProps {
  stats: any;
  loading: boolean;
}

function StorageStats({ stats, loading }: StorageStatsProps) {
  if (loading) {
    return <div className="storage-loading">Loading statistics...</div>;
  }

  if (!stats) {
    return <div className="storage-error">No statistics available</div>;
  }

  const sizeEstimate = (stats.total_memories || 0) * 0.05 + (stats.total_traces || 0) * 0.01;

  return (
    <div className="storage-stats">
      <div className="stat-card">
        <div className="stat-label">Total Memories</div>
        <div className="stat-value">{stats.total_memories || 0}</div>
        <div className="stat-sublabel">entries stored</div>
      </div>

      <div className="stat-card">
        <div className="stat-label">Total Traces</div>
        <div className="stat-value">{stats.total_traces || 0}</div>
        <div className="stat-sublabel">execution logs</div>
      </div>

      <div className="stat-card">
        <div className="stat-label">Estimated Size</div>
        <div className="stat-value">{sizeEstimate.toFixed(1)} MB</div>
        <div className="stat-sublabel">compressed storage</div>
      </div>

      <div className="stat-card">
        <div className="stat-label">Backend</div>
        <div className="stat-value">{stats.backend}</div>
        <div className="stat-sublabel">database type</div>
      </div>

      <div className="stat-card">
        <div className="stat-label">Compression</div>
        <div className="stat-value">40-70%</div>
        <div className="stat-sublabel">avg reduction</div>
      </div>

      <div className="stat-card">
        <div className="stat-label">Deduplication</div>
        <div className="stat-value">SHA-256</div>
        <div className="stat-sublabel">via hashing</div>
      </div>
    </div>
  );
}

interface AgentTracesProps {
  selectedAgent: string;
  onAgentChange: (agent: string) => void;
}

function AgentTraces({ selectedAgent, onAgentChange }: AgentTracesProps) {
  const { traces, loading, error, refetch } = useAgentTraces(selectedAgent, 50);

  return (
    <div className="agent-section">
      <div className="agent-selector">
        <label>Agent:</label>
        <input
          type="text"
          value={selectedAgent}
          onChange={(e) => onAgentChange(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && refetch()}
          placeholder="Enter agent name or ID"
        />
        <button onClick={refetch}>Load Traces</button>
      </div>

      {error && (
        <div className="storage-error">⚠️ {error}</div>
      )}

      {loading && (
        <div className="storage-loading">Loading traces...</div>
      )}

      {!loading && traces.length === 0 && (
        <div className="storage-empty">No traces found for this agent</div>
      )}

      {traces.length > 0 && (
        <div className="traces-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Agent</th>
                <th>Duration (ms)</th>
                <th>Timestamp</th>
                <th>Compressed</th>
              </tr>
            </thead>
            <tbody>
              {traces.map((trace) => (
                <tr key={trace.id}>
                  <td>{trace.id}</td>
                  <td>{trace.agent}</td>
                  <td>{trace.duration_ms}</td>
                  <td>{new Date(trace.timestamp).toLocaleString()}</td>
                  <td>{trace.is_compressed ? '✅' : '❌'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="table-footer">Showing {traces.length} traces</div>
        </div>
      )}
    </div>
  );
}

interface AgentMemoryViewProps {
  selectedAgent: string;
  onAgentChange: (agent: string) => void;
}

function AgentMemoryView({ selectedAgent, onAgentChange }: AgentMemoryViewProps) {
  const { memories, loading, error, refetch } = useAgentMemory(selectedAgent, 50);

  return (
    <div className="agent-section">
      <div className="agent-selector">
        <label>Agent:</label>
        <input
          type="text"
          value={selectedAgent}
          onChange={(e) => onAgentChange(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && refetch()}
          placeholder="Enter agent name or ID"
        />
        <button onClick={refetch}>Load Memory</button>
      </div>

      {error && (
        <div className="storage-error">⚠️ {error}</div>
      )}

      {loading && (
        <div className="storage-loading">Loading memory...</div>
      )}

      {!loading && memories.length === 0 && (
        <div className="storage-empty">No memories found for this agent</div>
      )}

      {memories.length > 0 && (
        <div className="memories-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Agent</th>
                <th>Summary</th>
                <th>Timestamp</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {memories.map((mem) => (
                <tr key={mem.id}>
                  <td>{mem.id}</td>
                  <td>{mem.agent}</td>
                  <td className="memory-summary">{mem.summary || '(no summary)'}</td>
                  <td>{new Date(mem.timestamp).toLocaleString()}</td>
                  <td>{mem.is_archived ? '📦 Archived' : '✅ Active'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="table-footer">Showing {memories.length} memories</div>
        </div>
      )}
    </div>
  );
}
