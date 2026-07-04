import { useState } from 'react';
import { apiFetch } from '../lib/api';

export function ProjectGeneratorDemo() {
  const [projectType, setProjectType] = useState('react-auth');
  const [loading, setLoading] = useState(false);
  const [project, setProject] = useState<any>(null);
  const [error, setError] = useState('');

  const generateProject = async () => {
    setLoading(true);
    setError('');
    try {
      const endpoint = projectType === 'react-auth' 
        ? '/v1/demo/generate-react-auth'
        : '/v1/demo/generate-spring-api';
      
      const res = await apiFetch(endpoint, { method: 'POST' });
      const data = await res.json();
      setProject(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Generation failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="project-generator">
      <h2>Project Generator</h2>
      
      <div className="generator-controls">
        <select value={projectType} onChange={(e) => setProjectType(e.target.value)}>
          <option value="react-auth">React Authentication App</option>
          <option value="spring-api">Spring Boot REST API</option>
        </select>
        <button onClick={generateProject} disabled={loading}>
          {loading ? 'Generating...' : 'Generate Project'}
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {project && (
        <div className="project-result">
          <h3>{project.name}</h3>
          <p><strong>Type:</strong> {project.type}</p>
          <p><strong>Framework:</strong> {project.framework}</p>
          <p><strong>Files:</strong> {project.file_count}</p>
          <p><strong>Size:</strong> {(project.size_bytes / 1024).toFixed(2)} KB</p>
          <p><strong>Generated in:</strong> {project.elapsed_ms}ms</p>

          <div className="files-list">
            <h4>Generated Files:</h4>
            <ul>
              {Object.keys(project.files).map(filename => (
                <li key={filename}>
                  <code>{filename}</code>
                  <span className="file-size">
                    ({(project.files[filename].length / 1024).toFixed(2)} KB)
                  </span>
                </li>
              ))}
            </ul>
          </div>

          <div className="file-preview">
            <h4>Preview (package.json / pom.xml):</h4>
            <pre>
              <code>
                {project.files['package.json'] || project.files['pom.xml']}
              </code>
            </pre>
          </div>
        </div>
      )}

      <style>{`
        .project-generator {
          padding: 2rem;
          background: var(--color-surface);
          border-radius: 8px;
          margin-top: 2rem;
        }

        .generator-controls {
          display: flex;
          gap: 1rem;
          margin: 1.5rem 0;
        }

        .generator-controls select,
        .generator-controls button {
          padding: 0.75rem 1rem;
          border-radius: 4px;
          border: 1px solid var(--color-border);
          background: var(--color-surface);
          color: var(--color-text);
          font-size: 1rem;
          cursor: pointer;
        }

        .generator-controls button {
          background: var(--color-primary);
          color: white;
          border: none;
          font-weight: 600;
        }

        .generator-controls button:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        .error-message {
          padding: 1rem;
          background: rgba(220, 50, 50, 0.1);
          color: #dc3232;
          border-radius: 4px;
          margin: 1rem 0;
        }

        .project-result {
          margin-top: 2rem;
          padding: 1.5rem;
          background: var(--color-background);
          border-radius: 4px;
          border: 1px solid var(--color-border);
        }

        .files-list {
          margin: 1.5rem 0;
        }

        .files-list ul {
          list-style: none;
          padding: 0;
        }

        .files-list li {
          padding: 0.5rem;
          background: var(--color-surface);
          margin: 0.5rem 0;
          border-radius: 4px;
          display: flex;
          justify-content: space-between;
          align-items: center;
        }

        .files-list code {
          font-family: monospace;
          color: var(--color-primary);
        }

        .file-size {
          font-size: 0.85rem;
          color: var(--color-text-secondary);
        }

        .file-preview {
          margin-top: 1rem;
        }

        .file-preview pre {
          background: var(--color-surface);
          padding: 1rem;
          border-radius: 4px;
          overflow-x: auto;
          max-height: 400px;
          border: 1px solid var(--color-border);
        }

        .file-preview code {
          font-family: 'Courier New', monospace;
          font-size: 0.85rem;
          color: var(--color-text);
        }
      `}</style>
    </div>
  );
}
