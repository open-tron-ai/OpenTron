import React, { useState, useEffect } from 'react'
import { useStorage } from '../hooks/useStorage'
import '../styles/AgentsPage.css'

interface Agent {
  name: string
  subtitle: string
  skills: string[]
}

function AgentsPage() {
  const [agents, setAgents] = useState<{ [key: string]: Agent }>({})
  const [loading, setLoading] = useState(true)
  const { makeRequest } = useStorage()

  useEffect(() => {
    loadAgents()
  }, [])

  const loadAgents = async () => {
    try {
      const response = await makeRequest('/v1/agents/status') as any
      if (response && response.agents) {
        setAgents(response.agents as { [key: string]: Agent })
      }
    } catch (error) {
      console.error('Failed to load agents:', error)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return <div className="agents-page">Loading agents...</div>
  }

  // Get Tron (coordinator key is lowercase)
  const tronAgent = agents['coordinator'] as Agent | undefined
  
  // Get all other agents
  const specialistAgents = Object.entries(agents)
    .filter(([key]) => key.toLowerCase() !== 'coordinator')
    .reduce((acc: { [key: string]: Agent }, [key, value]) => ({ ...acc, [key]: value }), {})

  return (
    <div className="agents-page">
      <h1>Agent Statuses</h1>

      {/* TRON - TOP CENTER - FIRST */}
      {tronAgent && (
        <div className="tron-wrapper">
          <div className="agent-card tron-card">
            <div className="agent-header">
              <h2>Tron</h2>
              <p className="agent-subtitle">Coordinator</p>
            </div>
            <div className="skills-container">
              {tronAgent.skills.map((skill: string) => (
                <span key={skill} className="skill-tag">
                  {skill}
                </span>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* SPECIALIST AGENTS - BELOW */}
      {Object.keys(specialistAgents).length > 0 && (
        <>
          <h3 className="specialists-heading">Specialist Agents</h3>
          <div className="agents-grid">
            {Object.entries(specialistAgents).map(([key, agent]: [string, Agent]) => (
              <div key={key} className="agent-card specialist-card">
                <div className="agent-header">
                  <h3>{agent.name}</h3>
                  <p className="agent-subtitle">{agent.subtitle}</p>
                </div>
                <div className="skills-container">
                  {agent.skills.map((skill: string) => (
                    <span key={skill} className="skill-tag">
                      {skill}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      <button onClick={loadAgents} className="refresh-btn">
        Refresh
      </button>
    </div>
  )
}

export default AgentsPage
