<div align="center">

  <a href="https://github.com/TruFoox/HoneyWasp">
    <img src="https://i.postimg.cc/Nj0gNW45/IMG-1599.png" alt="Logo" width="200" height="200" />
  </a>

  <h3>HoneyWasp</h3>

  <p>
    A bot to automatically post to various social media services - THIS README IS CURRENTLY USELESS. WILL BE IMPROVED SOON
    <br/><br/>
    <a href="https://github.com/TruFoox/HoneyWasp/issues/new">Report Bug</a> ·
    <a href="https://github.com/TruFoox/HoneyWasp/issues/new">Request Feature</a>
  </p>

  <p>
    <img src="https://img.shields.io/github/downloads/TruFoox/HoneyWasp/total" alt="Downloads" />
    <img src="https://img.shields.io/github/stars/TruFoox/HoneyWasp?style=social" alt="Stars" />
    <img src="https://img.shields.io/github/issues/TruFoox/HoneyWasp" alt="Issues" />
  </p>

</div>

# Table Of Contents

* [About the Project](#about-the-project)
* [Getting Started](#getting-started)
  * [Discord Bot Setup](#discord-bot-setup)
  * [Instagram Setup](#instagram-setup)
  * [YouTube Setup](#youtube-setup)
* [Usage](#usage)
* [Built With](#built-with)
* [Help](#help)
* [Contributing](#contributing)
* [Author](#author)
* [Acknowledgements](#acknowledgements)

# About The Project

This is a simple, lightweight, yet powerful bot for Instagram, Youtube, and more! You can automatically post media of your choice, or you can have the bot take an image off reddit using D3vd's [Meme API](https://github.com/D3vd/Meme_Api)!

A few of our currently available features:
* Manual or Automatic media selection 
* Highly customizable config

# Getting Started

Download the latest HoneyWasp .zip from [here](https://github.com/TruFoox/HoneyWasp/releases/latest). You can launch HoneyWasp by opening Launch.exe.

**However, first you must follow the instructions in [Discord Bot Setup](#discord-bot-setup) before you can use the bot**

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
6. Finally, copy the new token it gives you into ```discord_bot_token``` under ``[General_Settings]`` in Config.ini

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
- An **Instagram Business or Creator Account** linked to the Facebook Page    

### Step 1: Set Up Facebook App

1. Go to the Facebook Developer Portal: https://developers.facebook.com/apps/
2. Click **“Create App”**
3. Choose an app name (this doesnt matter) → click **Next**
4. For your **Use Case**, choose **Other**
5. When asked for the app type, choose **Business**
6. Press **Create app**
7. Open your app settings, and in the left panel, go to **Add Product**, find **Instagram**, and click **Set Up**

### Step 2: Link Accounts Properly

1. Make sure your **Instagram account is a Business or Creator account**
2. Link your Instagram account to a **Facebook Page** if it isnt already
   - IG App → Settings → Account → Switch to Professional
   - Facebook Page → Settings → Link Instagram Account

### Step 3: Get Access Token

1. In your app, go to **Tools → Graph API Explorer**
2. Select your app, and under **User or Page** select **Get User Access Token**
3. Under **Permissions**, add:
   - `instagram_basic`
   - `pages_show_list`
   - `instagram_basic`
   - `publish_video`
   - `pages_read_engagement`
   - `instagram_content_publish`
4. Click **Generate Access Token** and log in to your account
5. Copy the access token it gives you

Once you have this short-lived token, you are almost done. Now you must convert it into a long-lived token

### Step 4: Get Long Lived Access Token

1. Go to https://developers.facebook.com/tools/debug/accesstoken
2. Paste your access token into the box and press **Debug**
3. Scroll down to the bottom and press "Extend Access Token"
4. It will give you a different access token, which will expire in 2 months instead of 1 hour.
5. Place the result inside the ``api_key`` under ``[Instagram_Settings]`` of Config.ini

# Help

Please DM me on my [bot's Instagram page](https://www.instagram.com/dank.ai.memer/) or DM me on Discord (@TruFoox) for questions. I will both assist you there and, if the question is common enough, I will answer it here!

# Usage

This bot generally requires zero input from the user while it is running, but YOU MUST MAKE SURE TO MONITOR WHAT THE BOT POSTS!

If you fail to do so, the bot could post something against Instagram's TOS. This can be minimized, however, by keeping NSFW disabled in the config and only choosing from subreddits with infrequent unmarked NSFW.

## Built With

Programmed entirely in Python3 using the Requests, Colorama, Pillow, and Numpy libraries

## Contributing

Contributions are what make the open source community such an amazing place to be learn, inspire, and create. Any contributions you make are **greatly appreciated**.
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

* **[D++](https://dpp.dev/)** by [Brainboxdotcc](https://github.com/brainboxdotcc) - D++ is used in this program to communicate with discord
* **[Libcurl](https://curl.se/libcurl/)** - Allows HTTP API calls in C++
* **[Inih](https://github.com/benhoyt/inih)** by [Ben Hoyt](https://github.com/benhoyt) - Simplified reading .ini config
* **[nlohmann/json](https://github.com/nlohmann/json)** by [Nlohmann](https://github.com/nlohmann) - Parses json from API calls
* **[MemeAPI](https://github.com/D3vd)** by [D3vd](https://github.com/D3vd) - Utilized to automatically grab images when the user doesnt choose a source
