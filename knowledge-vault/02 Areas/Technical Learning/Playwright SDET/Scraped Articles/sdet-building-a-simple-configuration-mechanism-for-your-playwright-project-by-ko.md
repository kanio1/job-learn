# SDET: Building a simple configuration mechanism for your Playwright project

Playwright itself has a very simple configuration out of the box in the playwright.config file. But what about making it more flexible? What about the possibility to change this configuration depending on the environment and an easy way to read it from any project place?

## Libraries we need to install

npm install dotenv joi

dotenv loads environment variables from a .env file. joi is a schema validation library for validating environment variables.

## Config resolver

Create config.ts with dotenv.config(), Joi validation schema, and a Config class with static readonly validated fields.

## It is time to use it

Import Config in playwright.config.ts and use Config.WORKERS, Config.HEADLESS_BROWSER, Config.BASE_URL, etc.

Use Config.USER_NAME and Config.PASSWORD in page objects and helpers.

