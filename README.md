# clj-tomato-bot

[![Build Status](https://travis-ci.org/heycalmdown/clj-tomato-bot.svg?branch=travis)](https://travis-ci.org/heycalmdown/clj-tomato-bot)
[![Circle CI](https://circleci.com/gh/heycalmdown/clj-tomato-bot.svg?style=shield&no-cache=2)](https://circleci.com/gh/heycalmdown/clj-tomato-bot)
[![Coverage Status](https://coveralls.io/repos/github/heycalmdown/clj-tomato-bot/badge.svg?branch=travis)](https://coveralls.io/github/heycalmdown/clj-tomato-bot?branch=travis)
[![Dependencies Status](https://jarkeeper.com/heycalmdown/clj-tomato-bot/status.svg)](https://jarkeeper.com/heycalmdown/clj-tomato-bot)

tomayto or tomahto

Pomodoro bot for Telegram.

## Usage

- /go - Start a pomodoro session
- /check - Check remaining time for the current session
- /count - Show how many sessions do you have
- /cancel - Stop this session
- /pause - Pause this session
- /resume - Resume previous paused session without losing timer

<img alt="telegram screenshot" src="https://raw.githubusercontent.com/heycalmdown/clj-tomato-bot/master/doc/sceenshot.png" width="480px">

## Features

- Can have a pomodoro session in a Telegram messenger
- Notify the current state of the session 
- Support multiple devices
- Show remaining time on the fly


## Not featured yet

- Sharing a pomodoro session with your friend
- Stack-based task management


## Terms

- Mode - one of :pomodoro or :relax
- Session - a pair of :pomodoro and :relax


## License

Copyright © 2016 Kei Son

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
