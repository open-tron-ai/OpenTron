# Frontend Sections for OpenClaw Configuration

## Current Navigation Structure

The frontend has a **Sidebar** with navigation to these main sections:

1. **Chat** — Main conversation interface
2. **Dashboard** — Cost comparison, energy metrics, savings
3. **Data Sources** — Connect Gmail, Slack, Google Drive, etc. (Already implemented)
4. **Agents** — Create and manage long-running agents (Already implemented)
5. **Logs** — View activity and trace history
6. **Settings** — App preferences, API keys, inference source, memory config (Already implemented)
7. **Get Started** — Onboarding guide

## Recommended Locations for OpenClaw Configuration

### **Option 1: Dedicated "Skills" Page (RECOMMENDED)**

**New navigation item**: Add a "Skills" page to the sidebar between "Data Sources" and "Agents".

**Location in sidebar**: `frontend/src/pages/SkillsPage.tsx`

**UI structure**:
```
Skills
├── Available sources (tabs or dropdown)
│   ├── Hermes Agent (Official)
│   ├── OpenClaw (Community)
│   └── GitHub (Custom)
│
├── Search & filter
│   ├── Text search
│   └── Regex pattern (e.g., "web3|crypto")
│
├── Discovered skills grid
│   ├── Skill name
│   ├── Type (pipeline / instructional / hybrid)
│   ├── Description
│   ├── Tags
│   └── Install button
│
├── Installed skills list
│   ├── Skill name
│   ├── Type badge
│   └── Remove button
│
└── Auto-sync configuration
    ├── Toggle auto-sync
    └── Configure per-source filters
```

**Navigation code location**: `frontend/src/components/Sidebar/Sidebar.tsx` (line ~79)

### **Option 2: Settings Tab (Current Implementation)**

Add a new tab section to the existing **Settings Page** at: `frontend/src/pages/SettingsPage.tsx`

**UI structure** (after existing sections):
```
Settings → Skills section
├── Skill sources
│   ├── Hermes toggle + category filters
│   ├── OpenClaw toggle + search pattern filter
│   └── GitHub custom repositories
│
├── Auto-sync configuration
│   ├── Enable/disable auto-sync on startup
│   ├── Sync interval selector
│   └── Last synced timestamp
│
├── Installed skills
│   ├── List of installed skills with types
│   ├── Quick install/remove buttons
│   └── Skill details link
│
└── Manual sync button
    └── "Sync now" with progress indicator
```

**Code location**: Add new `<Section>` component after line ~600 in `SettingsPage.tsx`

### **Option 3: Data Sources Enhancement**

Expand the existing **Data Sources Page** to include skills: `frontend/src/pages/DataSourcesPage.tsx`

**UI structure**:
```
Data Sources & Skills
├── Tabs: "Data Sources" | "Skills"
│
└── Skills tab
    ├── OpenClaw registry browser
    ├── Search with regex support
    ├── Bulk install interface
    └── Installed skills manager
```

## Already Implemented Similar Sections

### **Data Sources Page** (`DataSourcesPage.tsx`)
- ✓ Lists available connectors (Gmail, Slack, Google Drive, etc.)
- ✓ Shows connected status (green indicator)
- ✓ Inline setup panels with step-by-step instructions
- ✓ Input fields for credentials
- ✓ Connect/Remove buttons
- ✓ Status messages

**Pattern to follow for Skills section**:
```typescript
// Similar structure: SourceList component
├── Connected skills grid
├── Unconnected skills grid  
├── Inline install/config panels
└── Status indicators
```

### **Agents Page** (`AgentsPage.tsx`)
- ✓ Launch wizard modal for agent creation
- ✓ Agent templates selection
- ✓ Advanced settings panel
- ✓ Tool picker with categories and filters

**Pattern to follow for Skills section**:
```typescript
// Could reuse launch wizard pattern for skill installation
// Use similar ToolsPicker component
```

### **Settings Page** (`SettingsPage.tsx`)
- ✓ Organized sections with `<Section>` component
- ✓ `<SettingRow>` for each configuration item
- ✓ API key inputs with masked values
- ✓ Toggle switches
- ✓ Dropdown selects
- ✓ Save/confirmation feedback

**Best pattern to reuse**: This page structure is ideal for OpenClaw configuration

## Recommended Implementation Approach

### **Create a new "Skills" tab in Settings** (Lowest effort, high discoverability)

Add to `SettingsPage.tsx` after the "Data" section:

```
┌─ Settings Page ────────────────────────────┐
│                                             │
│ [Appearance] [Connection] [Models] [Keys]  │
│ [Tools] [Memory] [Model Defaults] [Speech] │
│ [Data] [Updates] [Skills] [About]          │
│         ^-- NEW TAB                         │
│                                             │
│ ┌─ Skills ──────────────────────────────┐  │
│ │ Skill Sources                          │  │
│ │ ├─ ☐ Hermes Agent (0 new)             │  │
│ │ ├─ ☑ OpenClaw (5 installed)           │  │
│ │ │   └─ Search pattern: [web3|crypto]  │  │
│ │ └─ ☐ GitHub (custom repos)            │  │
│ │                                        │  │
│ │ Auto-sync: [Toggle] Every 24 hours    │  │
│ │                                        │  │
│ │ [Sync Now]                             │  │
│ │                                        │  │
│ │ Installed Skills (5)                   │  │
│ │ ├─ etherscan (pipeline)     [Remove]  │  │
│ │ ├─ gas-tracker (pipeline)   [Remove]  │  │
│ │ ├─ contract-auditor (hybrid) [Remove]  │  │
│ │ ├─ research-and-summarize (pipeline) │  │
│ │ └─ code-explainer (instructional)     │  │
│ │                                        │  │
│ │ Last synced: 2 hours ago               │  │
│ └─────────────────────────────────────────┘  │
│                                             │
└─────────────────────────────────────────────┘
```

## Component Reuse Opportunities

| Component | Location | Reusable For |
|-----------|----------|--------------|
| `<Section>` | `SettingsPage.tsx` | Skills section container |
| `<SettingRow>` | `SettingsPage.tsx` | Individual skill configurations |
| `ToolsPicker` | `AgentsPage.tsx` | Skills source selector |
| `LaunchWizard` | `AgentsPage.tsx` | Skill installation wizard (optional) |
| Status indicators | `DataSourcesPage.tsx` | Connected/installed skill badges |
| Inline setup panels | `DataSourcesPage.tsx` | Skill search & install interface |

## Frontend API Endpoints Needed

The frontend would need to call these backend endpoints (already implemented in Java CLI):

```
GET    /v1/agents/templates          → Get skill sources (hermes, openclaw)
GET    /v1/agents/tools              → List available tools to install
POST   /v1/skill/sync                → Trigger sync
GET    /v1/skill/list                → Get installed skills
POST   /v1/skill/install             → Install a specific skill
DELETE /v1/skill/{name}              → Remove a skill
GET    /v1/skill/search?q=web3       → Search skills by pattern
```

## Expected User Workflow (In Settings → Skills Tab)

1. User navigates to **Settings → Skills**
2. Sees three skill sources (Hermes, OpenClaw, GitHub)
3. Checks OpenClaw checkbox
4. Enters search pattern: `"web3|crypto"`
5. Clicks **"Sync Now"** button
6. See progress: "Discovering skills from OpenClaw..."
7. List appears: etherscan, gas-tracker, token-bridge, etc.
8. Each skill has an **Install** button
9. After install, skill moves to "Installed Skills" section
10. Skill immediately available in agent system prompt
11. Can remove skills from installed list anytime

## Alternative: Full Skills Page

If you want a dedicated page with more space, create: `frontend/src/pages/SkillsPage.tsx`

**Update Sidebar** in `Sidebar.tsx` line ~87:
```typescript
const navItems = [
  // ... existing items ...
  { path: '/skills', icon: Sparkles, label: 'Skills' },
  // ... rest ...
];
```

**Update App router** in `frontend/src/App.tsx`:
```typescript
<Route path="skills" element={<SkillsPage />} />
```

## Summary

**Best approach**: Add a **Skills section to the existing Settings page**

- ✓ No UI disruption (one new section among existing ones)
- ✓ Consistent UX (reuse SettingRow, Section components)
- ✓ Already familiar location for configuration
- ✓ Minimal routing/navigation changes
- ✓ Follows existing pattern (similar to Memory, Tools sections)

**Where to add**: `frontend/src/pages/SettingsPage.tsx` after the "Updates" section (around line ~650)
