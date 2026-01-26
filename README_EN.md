# Worksnaps Plugin for PhpStorm

[🇷🇺 Русский](README.md) | [🇬🇧 English](README_EN.md)

---

![Worksnaps Plugin](misc/phpstorm-worksnaps.png)

![PhpStorm](https://img.shields.io/badge/PhpStorm-2022.3+-blue.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)
![Version](https://img.shields.io/badge/version-1.0.0-brightgreen.svg)

Display your Worksnaps time tracking statistics directly in the PhpStorm status bar.

## Features

- 📊 Real-time display of worked hours for today
- 🎯 Activity percentage with color coding (green ≥80%, yellow 60-79%, red <60%)
- ⏱️ Remaining time until target hours (configurable, default: 8 hours)
- 🔄 Automatic refresh every 60 seconds (configurable)
- 💾 Caching for reliability when API is unavailable
- ⚙️ Fully customizable display format

## Display Example

Status bar shows: `WS: 9:30 (+1:30) | 87%`

Where:
- `9:30` = worked hours today (hours:minutes, rounded to 10-minute intervals)
- `(+1:30)` = overtime 1 hour 30 minutes (green if overtime, red if remaining)
- `87%` = activity percentage (green for ≥80%, yellow for 60-79%, red for <60%)

## Requirements

- PhpStorm 2022.3 or later
- Active Worksnaps account with API access
- Java 17 or later (for plugin development only)

## Installation

### From JetBrains Marketplace (Coming Soon)

1. Open PhpStorm
2. Go to **Settings/Preferences → Plugins**
3. Search for "Worksnaps"
4. Click **Install**
5. Restart PhpStorm

### Manual Installation

1. Download the latest release from [Releases](https://github.com/curkan/phpstorm-plugin-worksnaps/releases)
2. In PhpStorm, go to **Settings/Preferences → Plugins**
3. Click the gear icon ⚙️ and select **Install Plugin from Disk...**
4. Select the downloaded `.zip` file
5. Restart PhpStorm

### Build from Source

```bash
# Clone the repository
git clone https://github.com/curkan/phpstorm-plugin-worksnaps.git
cd phpstorm-plugin-worksnaps

# Build the plugin
./gradlew buildPlugin

# The plugin will be in build/distributions/
```

## Configuration

### 1. Get Your API Token

1. Log in to your Worksnaps account
2. Go to **Profile & Settings → Web Service API**
3. Click "Show my API Token"
4. Copy the token

### 2. Get Your Project ID

You can find your project ID in two ways:

**Option A: From URL**
- Open your project in Worksnaps web interface
- The project ID is in the URL: `https://app.worksnaps.com/projects/YOUR_PROJECT_ID`

**Option B: Via API**
```bash
curl -u "YOUR_API_TOKEN:" https://api.worksnaps.com/api/projects.xml
```

### 3. Configure the Plugin

1. Open PhpStorm
2. Go to **Settings/Preferences → Tools → Worksnaps**
3. Enter your configuration:
   - **API Token** (required): Your Worksnaps API token
   - **Project ID** (required): Your Worksnaps project ID
   - **User ID** (optional): Leave empty to auto-detect
   - **Update Interval**: How often to refresh data (default: 60 seconds)
   - **Target Hours**: Your daily work goal (default: 8 hours)
   - **Display Options**: Choose what to show in the status bar

4. Click **Apply** and **OK**

## Usage

Once configured, the plugin will automatically display your Worksnaps statistics in the PhpStorm status bar.

### Status Bar Display

The widget shows:
- Prefix (customizable, default: "WS:")
- Worked time (if enabled)
- Remaining time in parentheses (if enabled)
- Activity percentage (if enabled)
- Warning indicator (⚠) if using cached data due to API error

### Click Actions

Click on the widget in the status bar to manually refresh the data.

### Display States

- `WS: N/A` - API token or Project ID not configured
- `WS: Loading...` - Fetching data from API
- `WS: 4:50 (-3:10) | 78%` - Normal display
- `WS: 8:30 (+0:30) | 85%` - Overtime (30 minutes over target)
- `WS: 4:50 (-3:10) | 78% ⚠` - Using cached data due to API error
- `WS: ⚠ Error` - API error and no cached data available

## Settings Reference

| Setting | Default | Description |
|---------|---------|-------------|
| API Token | (empty) | Your Worksnaps API token (required) |
| Project ID | (empty) | Your Worksnaps project ID (required) |
| User ID | (empty) | Your user ID (optional, auto-detected if empty) |
| Update Interval | 60 | Refresh interval in seconds |
| Target Hours | 8.0 | Daily work goal in hours |
| Prefix | "WS:" | Text shown before statistics |
| Show Time | ✓ | Display worked hours |
| Show Activity | ✓ | Display activity percentage |
| Show Remaining | ✓ | Display remaining time until target |

## Troubleshooting

### Widget shows "WS: N/A"

**Solution**: Configure your API token and project ID in **Settings → Tools → Worksnaps**

### Widget shows "WS: ⚠ Error"

Possible causes:
- Invalid API token
- Invalid project ID
- No internet connection
- Worksnaps API is down

**Solution**:
1. Verify your API token is correct
2. Verify your project ID is correct
3. Check your internet connection
4. Test API manually:
   ```bash
   curl -u "YOUR_TOKEN:" https://api.worksnaps.com/api/projects.xml
   ```

### Data not updating

**Solution**:
1. Click on the widget to manually refresh
2. Check the update interval in settings
3. Restart PhpStorm

### Wrong activity percentage

**Note**: Worksnaps calculates activity based on keyboard and mouse usage. The plugin shows the average activity across all 10-minute time entries for today.

## How It Works

1. Plugin fetches time entries from Worksnaps API for the current day
2. Calculates total worked hours and average activity percentage
3. Results are cached for 60 seconds
4. If API is unavailable, uses cached data with warning indicator
5. Status bar updates automatically based on configured interval

## API Usage

- **Endpoint**: `https://api.worksnaps.com/api/projects/{project_id}/time_entries.xml`
- **Authentication**: HTTP Basic Auth (API token as username, empty password)
- **Rate Limiting**: Plugin uses caching to minimize API requests
- **Data Format**: XML (parsed using regex)

## Privacy & Security

- Your API token is stored securely in PhpStorm's credential store
- Plugin only reads time entry data for your configured project
- No data is sent to third parties
- All API communication is over HTTPS

## Development

### Setup

```bash
# Clone repository
git clone https://github.com/curkan/phpstorm-plugin-worksnaps.git
cd phpstorm-plugin-worksnaps

# Run plugin in development mode
./gradlew runIde
```

### Project Structure

```
src/main/kotlin/com/github/curkan/worksnaps/
├── api/
│   └── WorksnapsApiClient.kt       # API client for Worksnaps
├── service/
│   └── WorksnapsService.kt         # Service with caching and auto-refresh
├── settings/
│   ├── WorksnapsSettings.kt        # Persistent settings
│   └── WorksnapsConfigurable.kt    # Settings UI
├── ui/
│   ├── WorksnapsStatusBarWidget.kt # Status bar widget
│   └── WorksnapsStatusBarWidgetFactory.kt
└── listeners/
    └── ProjectOpenListener.kt      # Auto-start on project open
```

### Building

```bash
# Build plugin
./gradlew buildPlugin

# Run tests
./gradlew test

# Run verification
./gradlew runPluginVerifier
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Related Projects

- [tmux-plugin-worksnaps](https://github.com/curkan/tmux-plugin-worksnaps) - Worksnaps plugin for tmux

## License

MIT License - see [LICENSE](LICENSE) file for details

## Acknowledgments

- Inspired by [tmux-plugin-worksnaps](https://github.com/curkan/tmux-plugin-worksnaps)
- Built with [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/)

## Support

If you encounter issues, please report them on GitHub:
https://github.com/curkan/phpstorm-plugin-worksnaps/issues

## Changelog

### 1.0.0 (Initial Release)

- Display worked hours in status bar
- Show activity percentage with color coding
- Show remaining time until target
- Configurable settings
- Auto-refresh with caching
- Click to manually refresh
