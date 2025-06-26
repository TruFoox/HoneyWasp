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

Download the latest version of HoneyWasp from [here](https://github.com/TruFoox/HoneyWasp/releases/latest). You can launch HoneyWasp by opening Launch.exe.

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
4. Go to the **Bot** tab → click **Add Bot**
5. Finally, in the **Bot** tab, → click **Reset Token**, follow the instructions, and copy the token it gives you into ```discord_bot_token``` in Config.ini

### Step 2: Set OAuth2 Scopes & Permissions

Go to **OAuth2 → URL Generator**:

**Scopes** (ADD BOTH OF THESE):

- `bot`  
- `applications.commands`

**Bot Permissions** (ADD BOTH OF THESE):

- `Use Slash Commands`
- `Send Messages`

Copy the generated **invite URL**, paste it into your browser, and invite the bot to your server. You can now use the bot in its most basic form. To use it with Instagram or YouTube, you must follow the instructions below

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

* **[Brainboxdotcc](https://github.com/brainboxdotcc)** by [D++](https://dpp.dev/) - D++ is used in this program to communicate with discord
* **[Libcurl](https://curl.se/libcurl/)** - Allows HTTP API calls in C++
* **[nlohmann/json](https://github.com/nlohmann/json)** by [Nlohmann](https://github.com/nlohmann) - Parses json from API calls
* **[MemeAPI](https://github.com/D3vd)** by [D3vd](https://github.com/D3vd) - Utilized to automatically grab images when the user doesnt choose a source
