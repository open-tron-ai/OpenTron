export interface SetupStep {
  label: string;
  url?: string;
  urlLabel?: string;
}

export interface ConnectorMeta {
  connector_id: string;
  display_name: string;
  auth_type: 'oauth' | 'local' | 'bridge' | 'filesystem';
  category: 'communication' | 'documents' | 'pim' | 'other';
  icon: string;
  color: string;
  description: string;
  unitLabel?: string;
  steps?: SetupStep[];
  troubleshooting?: string[];
  inputFields?: Array<{
    name: string;
    placeholder: string;
    type?: 'text' | 'password';
  }>;
}

export interface ConnectorInfo {
  connector_id: string;
  display_name: string;
  auth_type: "oauth" | "local" | "bridge" | "filesystem";
  connected: boolean;
  auth_url?: string;
  mcp_tools?: string[];
  chunks?: number;
}

export interface SyncStatus {
  state: "idle" | "syncing" | "paused" | "error";
  items_synced: number;
  items_total: number;
  new_items_synced?: number | null;
  oldest_item_date?: string | null;
  last_sync: string | null;
  error: string | null;
}

export interface ConnectRequest {
  path?: string;
  token?: string;
  code?: string;
  email?: string;
  password?: string;
  clientId?: string;
  clientSecret?: string;
}

export interface ConnectResponse {
  connector_id: string;
  connected: boolean;
  status: "connected" | "pending" | "oauth_required" | "disconnected";
  oauth_start?: string;
  sync_status?: string | null;
}

export type WizardStep = "pick" | "connect" | "ingest" | "ready";
export type SourceCard = ConnectorMeta;
export type ConnectorCategory = ConnectorMeta['category'];

export const SOURCE_CATALOG: ConnectorMeta[] = [
  // ── Upload / Paste ──────────────────────────────────────────────────
  {
    connector_id: 'upload',
    display_name: 'Upload / Paste',
    auth_type: 'filesystem',
    category: 'other',
    icon: 'FileUp',
    color: 'text-blue-400',
    description: 'Paste text or upload documents',
    unitLabel: 'documents',
    steps: [
      { label: 'Paste text or upload files (.txt, .md, .pdf, .docx, .csv) to add them to your knowledge base.' },
    ],
    inputFields: [],
  },

  // ── Gmail ────────────────────────────────────────────────────────────
  {
    connector_id: 'gmail_imap',
    display_name: 'Gmail',
    auth_type: 'oauth',
    category: 'communication',
    icon: 'Mail',
    color: 'text-red-400',
    description: 'Email messages and threads',
    unitLabel: 'emails',
    steps: [
      {
        label: 'Make sure 2-Step Verification is enabled, then generate a 16-character App Password (Mail / Other / "OpenTron"). Paste it below \u2014 use the app password, not your regular Gmail password.',
        url: 'https://myaccount.google.com/apppasswords',
        urlLabel: 'How to get an app password \u2192',
      },
    ],
    troubleshooting: [
      "Don't see App Passwords? Make sure 2-Step Verification is enabled first.",
      "Google Workspace user? Your admin may need to enable App Passwords for your organization.",
    ],
    inputFields: [
      { name: 'email',    placeholder: 'you@gmail.com',                          type: 'text'     },
      { name: 'password', placeholder: 'App password (xxxx xxxx xxxx xxxx)',      type: 'password' },
    ],
  },

  // ── Slack ────────────────────────────────────────────────────────────
  {
    connector_id: 'slack',
    display_name: 'Slack',
    auth_type: 'oauth',
    category: 'communication',
    icon: 'Hash',
    color: 'text-purple-400',
    description: 'Read messages from every channel, private channel, DM, and group DM you have access to',
    unitLabel: 'messages',
    steps: [
      {
        label: 'Go to api.slack.com/apps and click "Create New App" \u2192 choose "From scratch". Name it "OpenTron" and pick your workspace',
        url: 'https://api.slack.com/apps',
        urlLabel: 'Open Slack Apps',
      },
      {
        label: 'In the left sidebar, click "OAuth & Permissions". Scroll down to "User Token Scopes" (NOT "Bot Token Scopes"). Add each scope: channels:history, channels:read, groups:history, groups:read, im:history, im:read, mpim:history, mpim:read, users:read',
      },
      {
        label: 'Click "Install App" \u2192 "Install to Workspace" \u2192 "Allow". Copy the "User OAuth Token" (starts with xoxp-, NOT xoxb-)',
      },
      {
        label: 'Paste the user token below.',
      },
    ],
    inputFields: [
      { name: 'token', placeholder: 'xoxp-...', type: 'password' },
    ],
  },

  // ── Notion ───────────────────────────────────────────────────────────
  {
    connector_id: 'notion',
    display_name: 'Notion',
    auth_type: 'oauth',
    category: 'documents',
    icon: 'FileText',
    color: 'text-gray-300',
    description: 'Pages and databases',
    unitLabel: 'pages',
    steps: [
      {
        label: 'Go to notion.so/profile/integrations \u2192 click "+ New integration". Name it "OpenTron", select your workspace, and click Submit.',
        url: 'https://www.notion.so/profile/integrations',
        urlLabel: 'Open Notion Integrations',
      },
      {
        label: 'Copy the "Internal Integration Secret" (starts with ntn_) and paste it below.',
      },
      {
        label: 'Share pages: open any top-level page \u2192 click "..." (top right) \u2192 "Connections" \u2192 "Add connections" \u2192 search "OpenTron". This shares the page and all sub-pages.',
      },
    ],
    inputFields: [
      { name: 'token', placeholder: 'ntn_...', type: 'password' },
    ],
  },

  // ── Granola ──────────────────────────────────────────────────────────
  {
    connector_id: 'granola',
    display_name: 'Granola',
    auth_type: 'oauth',
    category: 'documents',
    icon: 'Mic',
    color: 'text-amber-400',
    description: 'AI meeting notes',
    unitLabel: 'meeting notes',
    steps: [
      { label: 'Open the Granola desktop app. Click the gear icon (Settings) in the bottom-left corner, then click "API".' },
      { label: 'Click "Generate API Key" (or copy your existing key). Paste the key below.' },
    ],
    inputFields: [
      { name: 'token', placeholder: 'grn_...', type: 'password' },
    ],
  },

  // ── iMessage ─────────────────────────────────────────────────────────
  {
    connector_id: 'imessage',
    display_name: 'iMessage',
    auth_type: 'local',
    category: 'communication',
    icon: 'MessageSquare',
    color: 'text-green-400',
    description: 'macOS Messages history',
    unitLabel: 'messages',
    steps: [
      {
        label: 'Open System Settings \u2192 Privacy & Security \u2192 Full Disk Access.',
      },
      {
        label: 'Click "+" and add Terminal.app (or your terminal). If using the desktop app, also add OpenTron.app.',
      },
      {
        label: 'Toggle the switch ON. Restart your terminal or OpenTron. iMessage is detected automatically \u2014 no credentials needed.',
      },
    ],
  },

  // ── Obsidian ─────────────────────────────────────────────────────────
  {
    connector_id: 'obsidian',
    display_name: 'Obsidian',
    auth_type: 'filesystem',
    category: 'documents',
    icon: 'FolderOpen',
    color: 'text-purple-300',
    description: 'Markdown vault',
    unitLabel: 'notes',
    steps: [
      {
        label: 'Find your vault path: open Obsidian \u2192 click the vault name (bottom-left) \u2192 "Manage Vaults". On macOS it is usually ~/Documents/MyVault.',
      },
      {
        label: 'Paste the full path below. OpenTron will index all .md files in the vault.',
      },
    ],
    inputFields: [
      { name: 'path', placeholder: '/Users/you/Documents/MyVault', type: 'text' },
    ],
  },

  // ── Google Drive ─────────────────────────────────────────────────────
  {
    connector_id: 'gdrive',
    display_name: 'Google Drive',
    auth_type: 'oauth',
    category: 'documents',
    icon: 'FolderOpen',
    color: 'text-blue-400',
    description: 'Docs, Sheets, and files',
    unitLabel: 'files',
    steps: [
      {
        label: 'Go to Google Cloud Console \u2192 create a new project (or select an existing one).',
        url: 'https://console.cloud.google.com/projectcreate',
        urlLabel: 'Create Project',
      },
      {
        label: 'Enable the Google Drive API.',
        url: 'https://console.cloud.google.com/apis/library/drive.googleapis.com',
        urlLabel: 'Enable Drive API',
      },
      {
        label: 'Go to Credentials \u2192 "+ Create Credentials" \u2192 "OAuth client ID" \u2192 Application type: "Web application". Under "Authorized redirect URIs" add exactly: http://localhost:8000/v1/connectors/gdrive/oauth/callback \u2192 click "Create".',
        url: 'https://console.cloud.google.com/apis/credentials',
        urlLabel: 'Open Credentials',
      },
      {
        label: 'Copy the Client ID and Client Secret from the dialog (or the download icon). Paste them below and click Connect \u2014 a Google sign-in window will open.',
      },
    ],
    inputFields: [
      { name: 'clientId',     placeholder: 'Client ID',     type: 'text'     },
      { name: 'clientSecret', placeholder: 'Client Secret', type: 'password' },
    ],
  },

  // ── Google Calendar ──────────────────────────────────────────────────
  {
    connector_id: 'gcalendar',
    display_name: 'Google Calendar',
    auth_type: 'oauth',
    category: 'pim',
    icon: 'Calendar',
    color: 'text-blue-400',
    description: 'Events and meetings',
    unitLabel: 'events',
    steps: [
      {
        label: 'Go to Google Cloud Console \u2192 use the same project as Google Drive (or create a new one).',
        url: 'https://console.cloud.google.com/projectcreate',
        urlLabel: 'Open Console',
      },
      {
        label: 'Enable the Google Calendar API.',
        url: 'https://console.cloud.google.com/apis/library/calendar-json.googleapis.com',
        urlLabel: 'Enable Calendar API',
      },
      {
        label: 'Go to Credentials \u2192 "+ Create Credentials" \u2192 "OAuth client ID" \u2192 Application type: "Web application". Under "Authorized redirect URIs" add: http://localhost:8000/v1/connectors/gcalendar/oauth/callback \u2192 click "Create". You can reuse the same OAuth client as Google Drive by adding this URI to the existing client.',
        url: 'https://console.cloud.google.com/apis/credentials',
        urlLabel: 'Open Credentials',
      },
      {
        label: 'Paste the Client ID and Client Secret below.',
      },
    ],
    inputFields: [
      { name: 'clientId',     placeholder: 'Client ID',     type: 'text'     },
      { name: 'clientSecret', placeholder: 'Client Secret', type: 'password' },
    ],
  },

  // ── Google Contacts ──────────────────────────────────────────────────
  {
    connector_id: 'gcontacts',
    display_name: 'Google Contacts',
    auth_type: 'oauth',
    category: 'pim',
    icon: 'Users',
    color: 'text-blue-400',
    description: 'People and contact info',
    unitLabel: 'contacts',
    steps: [
      {
        label: 'Go to Google Cloud Console \u2192 use the same project as Google Drive (or create a new one).',
        url: 'https://console.cloud.google.com/projectcreate',
        urlLabel: 'Open Console',
      },
      {
        label: 'Enable the People API.',
        url: 'https://console.cloud.google.com/apis/library/people.googleapis.com',
        urlLabel: 'Enable People API',
      },
      {
        label: 'Go to Credentials \u2192 "+ Create Credentials" \u2192 "OAuth client ID" \u2192 Application type: "Web application". Under "Authorized redirect URIs" add: http://localhost:8000/v1/connectors/gcontacts/oauth/callback \u2192 click "Create".',
        url: 'https://console.cloud.google.com/apis/credentials',
        urlLabel: 'Open Credentials',
      },
      {
        label: 'Paste the Client ID and Client Secret below.',
      },
    ],
    inputFields: [
      { name: 'clientId',     placeholder: 'Client ID',     type: 'text'     },
      { name: 'clientSecret', placeholder: 'Client Secret', type: 'password' },
    ],
  },

  // ── Apple Notes ──────────────────────────────────────────────────────
  {
    connector_id: 'apple_notes',
    display_name: 'Apple Notes',
    auth_type: 'local',
    category: 'documents',
    icon: 'FileText',
    color: 'text-yellow-400',
    description: 'macOS Notes app',
    unitLabel: 'notes',
    steps: [
      { label: 'Open System Settings \u2192 Privacy & Security \u2192 Full Disk Access.' },
      { label: 'Click "+" and add Terminal.app. If using the desktop app, also add OpenTron.app.' },
      { label: 'Toggle the switch ON and restart. Apple Notes will be detected automatically \u2014 no credentials needed.' },
    ],
  },

  // ── Apple Contacts ───────────────────────────────────────────────────
  {
    connector_id: 'apple_contacts',
    display_name: 'Apple Contacts',
    auth_type: 'local',
    category: 'pim',
    icon: 'Users',
    color: 'text-orange-400',
    description: 'macOS Contacts app',
    unitLabel: 'contacts',
    steps: [
      { label: 'Open System Settings \u2192 Privacy & Security \u2192 Full Disk Access.' },
      { label: 'Click "+" and add Terminal.app. If using the desktop app, also add OpenTron.app.' },
      { label: 'Toggle the switch ON and restart. Apple Contacts will be detected automatically \u2014 no credentials needed.' },
    ],
  },

  // ── Outlook ──────────────────────────────────────────────────────────
  {
    connector_id: 'outlook',
    display_name: 'Outlook',
    auth_type: 'oauth',
    category: 'communication',
    icon: 'Mail',
    color: 'text-blue-400',
    description: 'Microsoft email and calendar',
    unitLabel: 'emails',
    steps: [
      {
        label: 'Go to Azure Portal \u2192 App Registrations \u2192 click "+ New registration". Name it "OpenTron", select "Accounts in any organizational directory and personal Microsoft accounts", and click Register.',
        url: 'https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade',
        urlLabel: 'Open Azure App Registrations',
      },
      {
        label: 'In the left sidebar, click "Authentication" \u2192 "Add a platform" \u2192 "Web". Under "Redirect URIs" add exactly: http://localhost:8000/v1/connectors/outlook/oauth/callback \u2192 click "Configure".',
      },
      {
        label: 'Click "API Permissions" \u2192 "Add a permission" \u2192 "Microsoft Graph" \u2192 "Delegated" \u2192 add Mail.Read and Calendars.Read.',
      },
      {
        label: 'Click "Certificates & secrets" \u2192 "New client secret" \u2192 set expiry \u2192 copy the Value immediately.',
      },
      {
        label: 'Go to "Overview" and copy the Application (client) ID. Paste both below.',
      },
    ],
    inputFields: [
      { name: 'clientId',     placeholder: 'Application (client) ID', type: 'text'     },
      { name: 'clientSecret', placeholder: 'Client Secret Value',      type: 'password' },
    ],
  },

  // ── Dropbox ──────────────────────────────────────────────────────────
  {
    connector_id: 'dropbox',
    display_name: 'Dropbox',
    auth_type: 'oauth',
    category: 'documents',
    icon: 'FolderOpen',
    color: 'text-blue-300',
    description: 'Cloud file storage',
    unitLabel: 'files',
    steps: [
      {
        label: 'Go to Dropbox App Console \u2192 "Create app" \u2192 "Scoped access" \u2192 "Full Dropbox". Give it a name and click "Create app".',
        url: 'https://www.dropbox.com/developers/apps/create',
        urlLabel: 'Open Dropbox App Console',
      },
      {
        label: 'In the "Settings" tab, under "OAuth 2" \u2192 "Redirect URIs" add exactly: http://localhost:8000/v1/connectors/dropbox/oauth/callback \u2192 click "Add".',
      },
      {
        label: 'Click the "Permissions" tab. Check "files.metadata.read" and "files.content.read" \u2192 click "Submit".',
      },
      {
        label: 'Go back to "Settings". Copy the App key (Client ID) and App secret (Client Secret). Paste them below.',
      },
    ],
    inputFields: [
      { name: 'clientId',     placeholder: 'App key (Client ID)',      type: 'text'     },
      { name: 'clientSecret', placeholder: 'App secret (Client Secret)', type: 'password' },
    ],
  },

  // ── WhatsApp ─────────────────────────────────────────────────────────
  {
    connector_id: 'whatsapp',
    display_name: 'WhatsApp',
    auth_type: 'oauth',
    category: 'communication',
    icon: 'MessageSquare',
    color: 'text-green-400',
    description: 'WhatsApp messages (Meta Cloud API)',
    unitLabel: 'messages',
    steps: [
      {
        label: 'Go to Meta for Developers \u2192 "Create App" \u2192 "Business" type.',
        url: 'https://developers.facebook.com/apps/',
        urlLabel: 'Open Meta Developer Portal',
      },
      {
        label: 'On the dashboard, find "WhatsApp" \u2192 "Set up". Add a test number and go to "API Setup". Copy the temporary access token.',
      },
      {
        label: 'Copy your Phone Number ID and the access token. Paste them below as: PhoneNumberID:AccessToken',
      },
    ],
    inputFields: [
      { name: 'token', placeholder: 'PhoneNumberID:AccessToken', type: 'password' },
    ],
  },
];
