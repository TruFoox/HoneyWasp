<br/>
<p align="center">
  <a href="https://github.com/TruFoox/HoneyWasp">
    <img src="https://media.discordapp.net/attachments/1006651353529073746/1377426576333078588/IMG_1599.PNG?ex=6852a137&is=68514fb7&hm=980afd1604aaf024a122814c986109e0c194d7c6a84b25d73414dda36ce6d15e&=&format=webp&quality=lossless&width=958&height=958" alt="Logo" width="200" height="200">

  </a>

  <h3 align="center">HoneyWasp</h3>

  <p align="center">
    A bot to automatically post to various social media services
    <br/>
    <br/>
    <a href="https://github.com/TruFoox/HoneyWasp/issues">Report Bug</a>
    .
    <a href="https://github.com/TruFoox/HoneyWasp/issues">Request Feature</a>
  </p>
</p>

![Downloads](https://img.shields.io/github/downloads/TruFoox/HoneyWasp/total) ![Stargazers](https://img.shields.io/github/stars/TruFoox/HoneyWasp?style=social) ![Issues](https://img.shields.io/github/issues/TruFoox/HoneyWasp) 

## Table Of Contents

* [About the Project](#about-the-project)
* [Getting Started](#getting-started)
  * [Instagram](#instagram)
* [Usage](#usage)
* [Built With](#built-with)
* [Help](#help)
* [Contributing](#contributing)
* [Authors](#authors)
* [Acknowledgements](#acknowledgements)

## About The Project

This is a simple, lightweight, yet powerful bot for Instagram, Youtube, and more! You can automatically post media of your choice, or you can have the bot rip a post off reddit using D3vd's [Meme API](https://github.com/D3vd/Meme_Api)!

A few of our currently available features:
* Manual or Automatic media selection 
* Highly customizable config

## Getting Started

Please [download the latest release](https://github.com/TruFoox/HoneyWasp/releases/latest) and edit the config.ini options listed below, depending on the service you want to use.

**THE BOT WILL NOT WORK IF YOU DO NOT PROVIDE THE APPROPRIATE CONFIG OPTIONS**


### Instagram

1. Go to [this URL](https://developers.facebook.com/tools/explorer/)
  
2. Press the blue "Generate Access Token" button. It will ask you to log in to your Facebook account, which is required. Make sure you log into whichever Facebook account owns the Instagram account you intend to use.

3. Go to [this URL](https://developers.facebook.com/tools/debug/accesstoken) and input the access token you just generated

4. Press the blue "Debug" button. After the new webpage loads, scroll down to the bottom and press "Extend Access Token"

5. It will give you a different access token, which will last much longer than an ordinary access token. Place the result inside ```api_key``` under ```[Instagram_Settings]``` of config.ini:
```api_key = PUT_API_KEY_HERE```

ASSITANCE ON ADDING USERID TO BE ADDED AT A LATER DATE

## Help

Please DM me on Discord (@TruFoox) for questions. I will assist you there and, if the question is common enough, I will answer it here as an FAQ!

## Usage

This bot generally requires zero input from the user while it is running, but when you have ```post_mode = auto``` in the config, you should keep an eye on the bot - it is still technically possible for it to post soemething against TOS

If you fail to do so, the bot could post something against the service's TOS. This can be minimized, however, by keeping NSFW disabled in the config and only choosing from subreddits with infrequent unmarked NSFW. (this is not a concern when the config is set to ```manual```.

## Built With

Programmed entirely in C++ using Libcurl (for API calls), D++ (for Discord integration), nlohmann/json (config parsing) & MemeAPI (for automatic image finding)

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
* **[nlohmann/json](https://github.com/nlohmann/json)** by [Nlohmann](https://github.com/nlohmann) - Parses json from API calls
* **[MemeAPI](https://github.com/D3vd)** by [D3vd](https://github.com/D3vd) - Utilized to automatically grab images when the user doesnt choose a source
