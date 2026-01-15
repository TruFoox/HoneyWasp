<p align="center">
  <a href="https://github.com/TruFoox/HoneyWasp">
    <img src="https://i.postimg.cc/Nj0gNW45/IMG-1599.png" alt="HoneyWasp Logo" width="200" height="200" /> 
  </a>
  <br /><br />
  <strong>HoneyWasp</strong>
  <br /><br />
  A bot to automatically post to various social media services.
  <br /><br />
  <a href="https://github.com/TruFoox/HoneyWasp/issues/new">Report Bug</a> · 
  <a href="https://github.com/TruFoox/HoneyWasp/issues/new">Request Feature</a>
  <br /><br />
  <img src="https://img.shields.io/github/downloads/TruFoox/HoneyWasp/total" alt="Downloads" />
  <img src="https://img.shields.io/github/stars/TruFoox/HoneyWasp?style=social" alt="Stars" />
  <img src="https://img.shields.io/github/issues/TruFoox/HoneyWasp" alt="Issues" />
</p>




# Table Of Contents

* [About the Project](#about-the-project)
* [Getting Started](#getting-started)
    * [Discord Bot Setup](#discord-bot-setup)
    * [Instagram Setup](#instagram-setup)
    * [YouTube Setup](#youtube-setup)
* [Usage](#usage)
    * [Customizing the Config](#the-config)
    * [Using the Bot](#starting-and-interacting-with-the-bot)
* [Help](#help)
    * [Windows Defender](#windows-defender-note)
* [Built With](#built-with)
* [Contributing](#contributing)
* [Author](#author)
* [Acknowledgements](#acknowledgements)

# About The Project

This is a simple, lightweight, yet powerful bot for Instagram, Youtube, and soon, more! You can automatically post media of your choice, or you can have the bot automatically take an image off reddit to post using D3vd's [Meme API](https://github.com/D3vd/Meme_Api)!

# Getting Started

Before you can run the bot, you’ll need **Java 23 or higher** installed.

- You can download the latest version of Java from [Oracle’s official site](https://www.oracle.com/java/technologies/downloads/) or use [Adoptium](https://adoptium.net/) for an open-source build. 
  - On Linux, you can run `sudo apt install openjdk-23-jdk`
- After installing, verify it’s working by running``java -version`` - It should show version 23 or higher.

- If you are running the bot on Mac or Linux, video support requires ffmpeg installed to your system PATH.
  - You can do this on linux with `sudo apt install ffmpeg`

After you have successfully confirmed you have Java 23+ installed, download the latest HoneyWasp .zip from [here](https://github.com/TruFoox/HoneyWasp/releases/latest). You can launch HoneyWasp by opening Launch.bat on Windows, or Launch.sh on Linux/Mac. 
- Alternatively, you can run the bot by running the command `java -jar HoneyWasp.jar` on any platform (The .bat/sh file just does it automatically).

**You must follow the instructions in [Discord Bot Setup](#discord-bot-setup) before you can use the bot in any capacity**

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
6. Finally, copy the new token it gives you into ``discord_bot_token`` under `[General_Settings]` in config.json

### Step 2: Set OAuth2 Permissions

Go to **OAuth2 → URL Generator**:

**Scopes** (ADD BOTH OF THESE):

- `bot`
- `applications.commands`

**Bot Permissions** (ADD BOTH OF THESE):

- `Use Slash Commands`
- `Send Messages`

Copy the generated **invite URL**, paste it into your browser, and invite the bot to your server. You can now use the bot in its most basic form.

To use it with Instagram or YouTube, go to either [Instagram Setup](#instagram-setup) or [YouTube Setup](#youtube-setup) for instructions

## Instagram Setup

### Prerequisites

- A Facebook (Meta) account
- A **Facebook Developer** account: https://developers.facebook.com
- A **Facebook Page**

### Step 1: Set Up Facebook App

1. Go to the Facebook Developer Portal: https://developers.facebook.com/apps/
2. Click **“Create App”**
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

### Step 3: Get Access Token

1. In your app, go to **Tools → Graph API Explorer**
2. Select your app, and under **User or Page** select **Get User Access Token**
3. Under **Permissions**, add:
    - `instagram_basic`
    - `pages_show_list`
    - `publish_video`
    - `instagram_content_publish`
4. Click Generate Access Token, then log in with your Facebook account and authorize access
5. Copy the access token it gives you

### Step 4: Get Long Lived Access Token

1. Go to https://developers.facebook.com/tools/debug/accesstoken
2. Paste your access token into the box and press **Debug**
3. Scroll down to the bottom and press "Extend Access Token"
4. It will give you a different access token, which will expire in 2 months instead of 1 hour.
5. Place the result inside the `api_key` under `[Instagram_Settings]` of config.json

## YouTube Setup

### Prerequisites

- A **Google Account**
- Access to the **Google Cloud Console**: https://console.cloud.google.com/
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

1. Go to **APIs & Services → Credentials**
2. Click **Create Credentials → OAuth client ID**
   - Set the **Application Type** as ``Desktop App``
4. If prompted, set up the OAuth consent screen:
    - Go to [this link](https://console.cloud.google.com/auth/audience) (Auth → Audience), and under **Publishing status**, press **Publish App**
    - Go to **APIs & Services → OAuth consent screen**
    - Fill in required fields (app name, support email. These do not matter)
    - Choose **External**, then press **Create**
    - Click **Save and Continue** until you can hit **Back to Dashboard**
5. Under **Create OAuth client ID**, choose **Desktop App**
6. Name it anything → click **Create**
7. Copy the **Client ID** and **Client Secret**
8. Place the Client ID in `client_id` and Client Secret in `client_secret` under `[Youtube_Settings]` of Config.json

### Step 4: Generate a Refresh Token (One-Time)
1. Run the bot by opening Launch.bat on Windows, or Launch.sh on Linux/Mac.
2. In the Discord Server where you have the bot, use `/start youtube`
3. Assuming `refresh_token` is empty in the config, the bot will attempt to open your web browser to allow you to retrieve your bot token
4. In this page, first select your Google account, then when prompted about the app being unverified → click **Show Advanced** → click **Go to [YOUR APP'S NAME] (unsafe)**
5. When prompted to allow access to your YouTube account, press **Continue**
6. You can find your access token in the URL, between `http://localhost/?code=` and `&scope=https://www.googleapis.com/auth/youtube.upload` (DO NOT INCLUDE THESE)
7. Your access token should look something like this: `4/0AJIL1DDF16...` (Do not include ANY component of the two parts of the URL listed in Step 6). Paste it into the console
8. Assuming your token was valid, the console will now give you a new token to put in `refresh_token` under `[Youtube_Settings]` of Config.json

# Usage
Below you can find information regarding [customizing the config](#the-config) and [using the bot](#starting-and-interacting-with-the-bot)
## The Config
Before launching the bot, make sure `Config.json` is set up correctly.  
All necessary fields (Credentials, API keys, etc.) should already be filled, assuming you followed [Instagram Setup](#instagram-setup), [YouTube Setup](#youtube-setup), or both.  
You can tweak the remaining settings, such as `post_mode`, `caption`, `autostart`, `subreddits`, and more to your preferences. Some may already be filled, but you can delete them, as they are placeholders.

All of the different config items are explained below

### Tips
- The config file is **not** forgiving of typos - what you input is taken literally, so check your spelling
- Boolean values (`true` or `false`) need to be lowercase (e.g., `"debug_mode": true`)
- There MUST be a comma delimiter after every config value
- ALWAYS surround string (non-numeric, non-boolean) values with quotes (e.g., `"caption": "Enjoy this meme"`)
- Do not put anything in `"refresh_token"` under `"Youtube_Settings"` until prompted to do so by the bot


### General_Settings
| Key                   | Description                                                                |
|-----------------------|----------------------------------------------------------------------------|
| `"discord_bot_token"` | Your bot's token for logging in to Discord                                 |
| `"webhook_url"`       | Optional Discord webhook URL for notifications                             |
| `"autostart"`         | List of services to launch automatically (e.g., `["instagram","youtube"]`) |
| `"debug_mode"`        | Enables verbose logging and debug output                                   |

### Instagram_Settings
| Key                                | Description                                                                                             |
|------------------------------------|---------------------------------------------------------------------------------------------------------|
| `"api_key"`                        | Instagram API key                                                                                       |
| `"post_mode"`                      | `"auto"` = grab posts from Reddit, `"manual"` = post from `/images` or `/videos` depending on post_mode |
| `"format"`                         | Determines how content is posted (`"video"` or `"image"`)                                               |
| `"audio_enabled"`                  | Whether to include audio when converting images to videos (add .MP3s to `/audio`)                       |
| `"time_between_posts"`             | Time, in minutes, between posts  (Instagram rate limits 25/day, per API key)                            |
| `"attempts_before_timeout"`        | The number of failed post attempts before giving up                                                     |
| `"hours_before_duplicate_removed"` | Time, in hours, before a post is allowed to be used again                                               |
| `"subreddits"`                     | Subreddits to pull content from (auto post_mode only, **exclude `r/`**)                                 |
| `"blacklist"`                      | Words that trigger this post to be discarded entirely (auto post_mode only)                             |
| `"duplicates_allowed"`             | Whether to allow duplicate posts                                                                        |
| `"nsfw_allowed"`                   | Whether to allow NSFW content (**FALSE HIGHLY recommended**)                                                   |
| `"use_reddit_caption"`             | Whether to use Reddit post title as the caption                                                         |
| `"caption_blacklist"`              | Words triggering fallback caption (auto post_mode only)                                                 |
| `"caption"`                        | Default post caption                                                                                    |
| `"hashtags"`                       | Hashtags appended to each Instagram post                                                                |

### YouTube_Settings
| Key                                | Description                                                                       |
|------------------------------------|-----------------------------------------------------------------------------------|
| `"refresh_token"`                  | Token for OAuth (**DO NOT PUT ANYTHING HERE UNTIL PROMPTED TO DO SO BY THE BOT**) |
| `"client_secret"`                  | YouTube API key for posting                                                       |
| `"client_id"`                      | YouTube user ID for API access (ends with `"apps.googleusercontent.com"`)         |
| `"post_mode"`                      | `"auto"` = grab posts from Reddit, `"manual"` = post from `/videos`               |
| `"audio_enabled"`                  | Whether to include audio when converting images to videos (add .MP3s to `/audio`) |  
| `"time_between_posts"`             | Time, in minutes, between posts (YouTube rate limits ~6/day, per API key)         |
| `"attempts_before_timeout"`        | The number of failed post attempts before giving up                               |
| `"hours_before_duplicate_removed"` | Time, in hours, before a post is allowed to be used again                         |
| `"subreddits"`                     | Subreddits to pull content from (auto post_mode only, **exclude `r/`**)           |
| `"blacklist"`                      | Words that trigger this post to be discarded entirely (auto post_mode only)       |
| `"duplicates_allowed"`             | Whether to allow duplicate posts                                                  |
| `"nsfw_allowed"`                   | Whether to allow NSFW content (**FALSE HIGHLY RECOMMENDED**)                      |
| `"use_reddit_caption"`             | Whether to use Reddit post title as the caption                                   |
| `"caption_blacklist"`              | Words that trigger fallback caption (auto post_mode only)                         |
| `"caption"`                        | Post caption                                                                      |
| `"description"`                    | Post description                                                                  |

## Starting and Interacting with the bot
To use the bot, open `Launch.bat` on Windows, or `Launch.sh` on Linux/Mac.
- Alternatively, you can run the bot by running the command `java -jar HoneyWasp.jar` on any platform (The .bat/sh file just does it automatically).

You can interact with the bot using Discord /slash commands. A list of commands can be found below:
- `/start [SERVICE/ALL]` - Start the bot on the specified service
- `/clear [SERVICE/ALL]` - Clear the automatic media cache for the specified service (Cache is used to prevent duplicate posts)
- `/stop [SERVICE/ALL]` - Stop the bot on the specified service

# Help

Please [Open an issue](https://github.com/TruFoox/HoneyWasp/issues/new) or DM me on Discord (@TruFoox) for questions

## Windows Defender Note
This app might get flagged by Windows Defender because automated programs that call apis can resemble certain types of malware, despite being safe.
If this happens, to use the bot you must allow it through Windows Defender (Or your specific antivirus, but below are instructions for Defender)

### How to Allow It Through Defender:
- Open Windows Security

- Go to Virus & threat protection

- Click "Protection history"

- Find the blocked app and click "Actions" > "Allow"

## Built With

Programmed with Java 23 in [IntelliJ IDEA](https://www.jetbrains.com/idea/), see [Acknowledgements](#acknowledgements)

## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.
* If you have suggestions for adding or removing projects, feel free to [open an issue](https://github.com/TruFoox/HoneyWasp/issues/new) to discuss it, or directly create a pull request after you edit the *README.md* file with necessary changes.
* Please make sure you check your spelling and grammar.
* Create individual PR for each suggestion.
* Please be a decent human being with your edits

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
* **[MemeAPI](https://github.com/D3vd/Meme_Api)** by [D3vd](https://github.com/D3vd) - Utilized to automatically grab images when the user doesn't choose a source
* **[0x0](https://0x0.su)** - Used to temporarily store videos for the bot to then send the URL to Instagram
