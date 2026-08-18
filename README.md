<p align="center">
  <a href="https://github.com/TruFoox/HoneyWasp">
    <img src="https://i.postimg.cc/Nj0gNW45/IMG-1599.png" alt="HoneyWasp Logo" width="200" height="200" /> 
  </a>
  <br /><br />
  <strong>HoneyWasp</strong>
  <br /><br />
  A Java-based bot for automating uploads to various social media services
  <br /><br />
  <a href="https://github.com/TruFoox/HoneyWasp/issues/new">Report Bug</a> · 
  <a href="https://github.com/TruFoox/HoneyWasp/issues/new">Request Feature</a>
  <br /><br />
  <img src="https://img.shields.io/github/downloads/TruFoox/HoneyWasp/total" alt="Downloads" />
  <img src="https://img.shields.io/github/stars/TruFoox/HoneyWasp" alt="Stars" />
  <img src="https://img.shields.io/github/issues/TruFoox/HoneyWasp" alt="Issues" />
</p>


# Table Of Contents

* [About the Project](#about-the-project)
* [Getting Started](#getting-started)
    * [Discord Bot Setup](#discord-bot-setup)
    * [Instagram Setup](#instagram-setup)
    * [YouTube Setup](#youtube-setup)
* [Usage](#usage)
    * [The Config](#the-config)
    * [Running the Bot](#starting-and-interacting-with-the-bot)
* [Help](#help)
    * [Windows Defender](#windows-defender-note)
* [Built With](#built-with)
* [Contributing](#contributing)
* [Author](#author)
* [Acknowledgements](#acknowledgements)

# About The Project

This is a simple, lightweight, yet powerful bot for Instagram, YouTube, and soon, more! You can automatically post media of your choice, or you can have the bot automatically take an image off reddit to post using D3vd's [Meme API](https://github.com/D3vd/Meme_Api)!

All data handling, keys, tokens, and processing is handled **client side** - Your information is **YOURS** and is never seen by a 3rd party

It is programmed with a polymorphic implementation to allow for easier contributions, so if you want to propose an edit, feel free to [make a pull request!](#creating-a-pull-request)
<p align="center">
  <img src="https://i.postimg.cc/Jnqhg1yy/image.png" alt="HoneyWasp" width="720" height="519" /> 
</p>

# Getting Started
Before you can run the bot, you’ll need **Java 23 or higher** installed:

- You can download the latest version of Java from [Oracle’s official site](https://www.oracle.com/java/technologies/downloads/) or use [Adoptium](https://adoptium.net/) for an open-source build. 
  - On Linux, you can run `sudo apt install openjdk-23-jdk`
  - On Mac, you can install [Homebrew](https://brew.sh/) and run `brew install openjdk@23`, or use the links above to install manually
- After installing, verify it’s working by running `java -version` in the console - It should show version 23 or higher.

Next, you will need FFmpeg:
- If you are running the bot on Mac or Linux, video support requires FFmpeg installed to your system PATH.
  - You can do this on Linux with `sudo apt install ffmpeg`
  - On Mac w/ [Homebrew](https://brew.sh/): `brew install ffmpeg`
  - After installing, verify it’s working by running `ffmpeg -version` in the console
- If you are on Windows, you are already provided with a copy of FFmpeg in the .zip and no further action is needed

After you have successfully confirmed you have Java 23+ and FFmpeg installed, download the latest HoneyWasp .zip from [here](https://github.com/TruFoox/HoneyWasp/releases/latest).

You must follow the instructions in either [Instagram Setup](#instagram-setup) or [YouTube Setup](#youtube-setup) to set up the bot to be able to use the bot in any capacity

It is **HIGHLY** recommended that you first follow the instructions in [Discord Bot Setup](#discord-bot-setup), so you can run the bot via commands
- If you decide not to use Discord, you **MUST** enable autostart for the services you want to run, and leave `discord_bot_token` under `[General_Settings]` empty
  - Without Discord, you will not be able to stop the service once it starts

Once you are finished setting up the bot, you can launch it by opening Launch.bat on Windows, or Launch.sh on Linux/Mac.
- Alternatively, you can run the bot by running the command `java -jar HoneyWasp.jar` on any platform (The .bat/sh file just does it automatically).

Help regarding the config can be [found here](#the-config), and more information on how to interact with the bot can be [found here](#starting-and-interacting-with-the-bot). If you are having issues with Windows defender wrongly flagging the bot as malicious, [you can find a fix here](#windows-defender-note).

## Discord Bot Setup

### Prerequisites

- A Discord account
- A Discord server (with permission to add bots) 
- [Discord Developer Portal](https://discord.com/developers/applications)

### Step 1: Create Your Application

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications)
2. Click **"New Application"**
3. Name it, then click **Create**
4. Click your new bot to open its settings
5. In the **Bot** tab in the left panel, → click **Reset Token**, follow the instructions to get your bot's API token
6. Finally, copy the new token it gives you into `discord_bot_token` under `[General_Settings]` in config.json

### Step 2: Set OAuth2 Permissions

1. Go to **OAuth2 → URL Generator**:

**Scopes** (ADD BOTH OF THESE):

- `bot`
- `applications.commands`

**Bot Permissions** (ADD BOTH OF THESE):

- `Use Slash Commands`
- `Send Messages`

2. Copy the generated **invite URL**, paste it into your browser, and invite the bot to your server. You can now use the bot in its most basic form.

### Step 3: Get the Webhook URL (OPTIONAL)

1. Open the Discord server where you want the bot to send messages.
2. Go to **Server Settings → Integrations → Webhooks**.
3. Click **New Webhook**.
4. Select the channel you want the bot to post in.
5. Click **Copy Webhook URL**.
6. Paste the URL into `webhook_url` under `[General_Settings]`

## Instagram Setup

### Prerequisites

- A Facebook (Meta) account
- A **Facebook Developer** account: https://developers.facebook.com
- An **Instagram Business Account** linked to your Facebook Account

### Step 1: Set Up Facebook App

1. Go to the Facebook Developer Portal: https://developers.facebook.com/apps/
2. Click **Create App**
3. Choose an app name (this doesn't matter) → click **Next**
4. For your **Use Case**, choose **Other**
5. When asked for the app type, choose **Business**
6. Press **Create app**
7. Open your app settings, and in the left panel, go to **Add Product**, find **Instagram**, and click **Set Up**

### Step 2: Link Accounts Properly

1. Make sure your **Instagram account is a Business or Creator account**
2. Link your Instagram account to a **Facebook Page** if it isn't already
    - IG App → Settings → Account → Switch to Professional
    - Facebook Page → Settings → Link Instagram Account

### Step 3: Get Temporary Access Token

1. Go to https://developers.facebook.com/tools/explorer/
2. Select your app, and under **User or Page** select **Get User Access Token**
3. Under **Permissions**, add:
    - `instagram_basic`
    - `pages_show_list`
    - `publish_video`
    - `instagram_content_publish`
4. Click Generate Access Token, then log in with your Facebook account and authorize access
5. Copy the access token it gives you

### Step 4: Convert Temporary Token into Long Lived Access Token

1. Go to https://developers.facebook.com/tools/debug/accesstoken
2. Paste your access token into the box and press **Debug**
3. Scroll down to the bottom and press "Extend Access Token"
4. It will give you a different access token, which will expire in 2 months instead of 1 hour.
5. Place the result inside `api_key` under `[Instagram_Settings]` of config.json
    - Approximately every 2 months you will need to repeat Steps 3 & 4 when the token expires


## YouTube Setup

### Prerequisites

- A **Google Account**
- A **YouTube Channel** linked to your Google Account

### Step 1: Create a Project in Google Cloud

1. Go to the Google Cloud Console: https://console.cloud.google.com/
2. Click the project dropdown at the top → **New Project**
3. Name it and click **Create**
4. After it's created, click the project dropdown again → select your new project

### Step 2: Enable YouTube Data API

1. Search for **YouTube Data API** or go [here](https://console.cloud.google.com/marketplace/product/google/youtube.googleapis.com?q=search&referrer=search&inv=1&invt=Ab2WDA&project=agile-falcon-356204)
2. Click on it, then press **Enable**

### Step 3: Set Up OAuth Credentials

1. Go to **APIs & services → Credentials**
2. Click **Create Credentials → OAuth client ID**
   - Set the **Application Type** as ``Desktop App``
3. If prompted, set up the OAuth consent screen:
    - Go to [this link](https://console.cloud.google.com/auth/audience) (Auth → Audience), and under **Publishing status**, press **Publish App**
    - Go to **APIs & services → OAuth consent screen**
    - Fill in required fields (app name, support email. These do not matter)
    - Choose **External**, then press **Create**
    - Click **Save and Continue** until you can hit **Back to Dashboard**
4. Under **Create OAuth client ID**, choose **Desktop App**
5. Name it anything → click **Create**
6. Copy the **Client ID** and **Client Secret**
7. Place the Client ID in `client_id` and Client Secret in `client_secret` under `[Youtube_Settings]` of Config.json

### Step 4: Generate a Refresh Token (One-Time)
1. Run the bot by opening Launch.bat on Windows, or Launch.sh on Linux/Mac, and run YouTube
2. Assuming `refresh_token` is empty in the config, the bot will attempt to open your web browser to allow you to retrieve your bot token
3. In this page, first select your Google account, then when prompted about the app being unverified → click **Show Advanced** → click **Go to [YOUR APP'S NAME] (unsafe)**
4. When prompted to allow access to your YouTube account, press **Continue**
5. You will be redirected to an empty page. Copy the URL of the page, and paste it into the console

# Usage
There are a few specific details about the bot you need to know before you use it.
* It only officially supports .mp3s for audio, .mp4s for video, and .jpg/png for images
	* Some other file types may work, but they are not accounted for and will not receive official support
* Only one instance of each type of service can run at one time
    * This is likely to change in the future, with multiple bot tokens being allowed
* Enabling `restart` may cause issues, and the option only exists for servers where crashes are few and far between
* If you are having issues, you should try enabling `debug_mode` under `General_Settings` in config.json to help pinpoint the issue

Knowing all this, you can now begin [customizing the config](#the-config), then [using the bot](#starting-and-interacting-with-the-bot)


## The Config
Before launching the bot, make sure `Config.json` is set up correctly.  
All necessary fields (Credentials, API keys, etc.) should already be filled, assuming you followed [Instagram Setup](#instagram-setup), [YouTube Setup](#youtube-setup), or both.  
You can tweak the remaining settings, such as `post_mode`, `caption`, `autostart`, `subreddits`, and more to your preferences. Some may already be filled, but you can delete them, as they are placeholders.

[An example config has been provided here](https://github.com/TruFoox/HoneyWasp/blob/master/example_config.json). If you are having issues, make sure your config has identical formatting to the example.

Below you can find documentation on every configuration option

### Tips
- The config file is **not** forgiving of typos - what you input is taken literally, so check your spelling
- Boolean values (`true` or `false`) need to be lowercase (e.g., `"debug_mode": true`)
- There MUST be a comma delimiter after every config value, except for the last one
- ALWAYS surround string (non-numeric, non-boolean) values with quotes (e.g., `"caption": "Enjoy this meme"`)
- List config values can be disabled by leaving them blank (e.g, `"blacklist": [""]` to disable the blacklist)
- Do not put anything in `"refresh_token"` under `"Youtube_Settings"` until prompted to do so by the bot


### General Settings
| Key                 | Description                                                                                           |
|---------------------|-------------------------------------------------------------------------------------------------------|
| `discord_bot_token` | Your bot's token for logging in to Discord (Optional but recommended, set to `""` to disable Discord) |
| `webhook_url`       | Discord webhook URL for notifications (Optional, set to `""` to disable)                              |
| `restart`           | An EXPERIMENTAL setting to enable bot to automatically restart bot on crash (`true` or `false`)       |
| `debug_mode`        | Enables verbose logging (`true` or `false`)                                                           |

### Instagram Settings
| Key                              | Description                                                                                                                                     |
|----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `api_key`                        | Instagram API key                                                                                                                               |
| `autostart`                      | Whether to automatically start Instagram when HoneyWasp starts (`true` or `false`)                                                              |
| `autopost_mode`                  | Whether to automatically fetch images from reddit. Set to `false` to post from `/images` or `/videos` based on `video_mode` (`true` or `false`) |
| `video_mode`                     | Determines how content is posted. Set to `true` to post media as video, with optional audio (`true` or `false`)                                 |
| `audio_enabled`                  | Whether to include audio when converting images to videos (`"autopost_mode": true` & `"video_mode": true` only, add .MP3s to `/audio`)          |
| `minutes_between_posts`          | Time, in minutes, between posts  (Instagram rate limits 25/day, per API key)                                                                    |
| `attempts_before_timeout`        | The number of failed post attempts before giving up. Set to 0 for infinite                                                                      |
| `hours_before_duplicate_removed` | Time, in hours, before a post is allowed to be used again. Set to 0 for never (`"autopost_mode": true` only)                                    |
| `subreddits`                     | Subreddits to pull content from. (**Exclude `r/`**, `"autopost_mode": true` only)                                                               |
| `blacklist`                      | Words that, if found,  trigger this post to be discarded entirely (`autopost_mode: true` only)                                                  |
| `duplicates_allowed`             | Whether to allow duplicate posts (`"autopost_mode": true` only, `true` or `false`)                                                              |
| `nsfw_allowed`                   | Whether to allow NSFW content (**FALSE HIGHLY RECOMMENDED**, `"autopost_mode": true` only, `true` or `false`)                                   |
| `use_reddit_caption`             | Whether to use Reddit post title as the caption (`"autopost_mode": true` only,`true` or `false`)                                                |
| `caption_blacklist`              | Words that, if found, trigger the bot to use `caption` instead of reddit caption (`"autopost_mode": true` & `"use_reddit_caption": true` only)  |
| `caption`                        | Default post caption                                                                                                                            |
| `hashtags`                       | Hashtags appended to post after caption                                                                                                         |

### YouTube Settings
| Key                              | Description                                                                                                                                       |
|----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `refresh_token`                  | Refresh token for OAuth (**DO NOT PUT ANYTHING HERE UNTIL PROMPTED TO DO SO BY THE BOT**)                                                         |
| `client_secret`                  | YouTube API key for posting                                                                                                                       |
| `client_id`                      | YouTube user ID for API access (ends with "apps.googleusercontent.com")                                                                           |
| `autostart`                      | Whether to automatically start YouTube when HoneyWasp starts (`true` or `false`)                                                                  |
| `autopost_mode`                  | Whether to automatically fetch images from reddit before converting them to videos. Set to `false` to post from `/videos`                         |
| `audio_enabled`                  | Whether to include audio when converting images to videos (`"autopost_mode": true` only, add .MP3s to `/audio`)                                   |  
| `minutes_between_posts`          | Time, in minutes, between posts (YouTube rate limits ~6/day, per API key)                                                                         |
| `attempts_before_timeout`        | The number of failed post attempts before giving up. Set to 0 for infinite                                                                        |
| `hours_before_duplicate_removed` | Time, in hours, before a post is allowed to be used again. Set to 0 for never (`"autopost_mode": true` only)                                      |
| `subreddits`                     | Subreddits to pull content from (**Exclude `r/`**, `"autopost_mode": true` only)                                                                  |
| `blacklist`                      | Words that trigger this post to be discarded entirely (`"autopost_mode": true` only)                                                              |
| `duplicates_allowed`             | Whether to allow duplicate posts (`"autopost_mode": true` only,`true` or `false`)                                                                 |
| `nsfw_allowed`                   | Whether to allow NSFW content (**FALSE HIGHLY RECOMMENDED**, `"autopost_mode": true` only, `true` or `false`)                                     |
| `use_reddit_caption`             | Whether to use Reddit post title as the caption (`"autopost_mode": true` only, `true` or `false`)                                                 |
| `caption_blacklist`              | Words that, if found, trigger the bot to use default caption instead of reddit post (`"autopost_mode": true` & `"use_reddit_caption": true` only) |
| `caption`                        | Default post title                                                                                                                                |
| `hashtags`                       | Post description                                                                                                                                  |

## Starting and Interacting with the bot
To use the bot, open `Launch.bat` on Windows, or `Launch.sh` on Linux/Mac.
- Alternatively, you can run the bot by running the command `java -jar HoneyWasp.jar` on any platform (The .bat/sh file just does it automatically).


There are two supported methods of running a service:
- Enabling `autostart` in the service you want to start
  - This will automatically start the service when HoneyWasp starts
  - This option is required if you chose not to use Discord
- Starting via `/start`
  - This is only supported if you followed the instructions under [Discord Bot Setup](#discord-bot-setup)


If you choose to use Discord, a list of commands can be found below:
- `/start [SERVICE/ALL]` - Start the bot on the specified service
- `/clear [SERVICE/ALL]` - Clear the automatic media cache for the specified service (Cache is used to prevent duplicate posts)
- `/stop [SERVICE/ALL]` - Stop the bot on the specified service
- `/status [SERVICE/ALL]` - Gets whether the specified service is running or not

# Help

Please [Open an issue](https://github.com/TruFoox/HoneyWasp/issues/new) or DM me on Discord (@TruFoox) for questions

## Windows Defender Note
This app might get flagged by Windows Defender because automated programs that call apis can resemble certain types of malware, despite being safe.
If this happens, to use the bot you must allow it through Windows Defender (Or your specific antivirus, but below are instructions for Defender)

### How to Allow It Through Defender:
- Open Windows Security

- Go to Virus & threat protection

- Click "Protection history"

- Find the blocked app and click "Actions" → "Allow"

## Built With

Programmed with Java 23 in [IntelliJ IDEA](https://www.jetbrains.com/idea/), see [Acknowledgements](#acknowledgements)

## Contributing

Contributions are what make the open source community such an amazing place to learn and create. Any contributions you make are **greatly appreciated**.
* If you have suggestions, feel free to [open an issue](https://github.com/TruFoox/HoneyWasp/issues/new) to discuss it, or directly create a pull request after you edit the *README.md* file with necessary changes.
* Please make sure you check your spelling and grammar.
* [Create individual pull request](#creating-a-pull-request) for each suggestion.

### Creating A Pull Request

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request


## Author

* [TruFoox](https://github.com/TruFoox/)

## Acknowledgements

* **[JDA](https://github.com/DV8FromTheWorld/JDA)** - Java Discord API used to communicate with Discord
* **[org.json](https://github.com/stleary/JSON-java)** - JSON parsing library for API calls and config handling
* **[Discord Webhooks](https://github.com/MinnDevelopment/Discord-Webhooks)** - For sending messages via Discord webhooks
* **[JavaCV](https://github.com/bytedeco/javacv)** - Java wrapper for OpenCV, used to convert photos to video
* **[Jackson](https://github.com/FasterXML/jackson-databind)** - JSON serialization/deserialization library for configs and API calls
* **[MemeAPI](https://github.com/D3vd/Meme_Api)** by [D3vd](https://github.com/D3vd) - Utilized to automatically grab images from Reddit when `auto_post_mode` enabled
* **[0x0](https://0x0.st)** - Used to temporarily store videos for the bot to then send the URL to Instagram
