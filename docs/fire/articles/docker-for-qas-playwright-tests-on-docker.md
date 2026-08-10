# Docker for QAs: Playwright Tests On Docker

| Field | Value |
|-------|-------|
| **Author** | rodrigoobc |
| **Published** | unknown |
| **URL** | https://dev.to/rodrigoobc/docker-for-qas-playwright-tests-on-docker-46cj |
| **Scraped with** | Firecrawl `firecrawl_scrape` (`formats: ["markdown"]`, `onlyMainContent: true`) |

## Firecrawl metadata

```json
{
  "title": "Docker for QAs: Playwright Tests On Docker - DEV Community",
  "og:title": "Docker for QAs: Playwright Tests On Docker",
  "og:url": "https://dev.to/rodrigoobc/docker-for-qas-playwright-tests-on-docker-46cj",
  "description": "In the dynamic world of QA, agility and reliability in testing are crucial. Docker emerges as a...",
  "statusCode": 200,
  "sourceURL": "https://dev.to/rodrigoobc/docker-for-qas-playwright-tests-on-docker-46cj"
}
```

---

In the dynamic world of QA, agility and reliability in testing are crucial. Docker emerges as a powerful ally, offering a standardized and replicable environment to automate your tests with Playwright. In this article, we will embark on a journey to optimize your tests with Docker, from installation to execution.

* * *

## Introduction to Docker

Docker was launched in 2013 by the company dotCloud, now known as Docker, Inc. Created by Solomon Hykes, Docker revolutionized the way developers and operations teams handle application deployment, providing an efficient and lightweight solution for creating, deploying, and running containers. This innovation has significantly facilitated the management of development and production environments, ensuring consistency and scalability.

Docker simplifies continuous integration and continuous delivery (CI/CD), enabling the automation of processes and reducing human errors. The efficiency in resource usage and the portability of containers also make Docker an ideal choice for development and operations teams looking to optimize their workflows and ensure system stability.

* * *

## Using Rancher Desktop

It's important to note that we will be using Rancher Desktop, not its more popular counterpart, Docker Desktop, because Rancher Desktop is completely open source. Docker Desktop is only free for creating other open source code or for students.

### Installing Rancher Desktop

Rancher Desktop is a useful tool for managing containers and Kubernetes clusters in a local development environment.

Below, we will detail the steps to install Rancher Desktop on your system:

**_Windows_**

1. Run the downloaded file (.exe).
2. Follow the installer instructions, accepting the terms of use and selecting the desired installation directory.
3. Complete the installation by clicking "Finish".

**_macOS_**

1. Open the downloaded file (.dmg).
2. Drag the Rancher Desktop icon to the "Applications" folder.
3. Open Rancher Desktop from the "Applications" folder and, if necessary, authorize its execution in System Preferences.

**_Linux_**

Download the appropriate file for your distro.

_Debian/Ubuntu:_

```
sudo dpkg -i /caminho/para/o/arquivo.deb
sudo apt-get install -f
```

_Fedora:_

```
sudo rpm -i /caminho/para/o/arquivo.rpm
```

### Configuring Rancher Desktop

After installing Rancher Desktop, we need to perform its initial configurations. First, we choose to work with Docker instead of the containerd version. Containerd is another technology used for the same purposes as Docker. However, in this article, we will focus more on using Docker. Upon completing the configuration, we will see the following initial screen:

![Home page Rancher Desktop](https://media2.dev.to/dynamic/image/width=800%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2F6vkxdpwhoqjhdsb40fc7.png)

1. **Containers**: This is the section in Rancher Desktop where we can find all the containers we have, whether active or not.
2. **Images**: This is the section in Rancher Desktop where we can find all the downloaded images on our computer.
3. **Preferences**: This is the section in Rancher Desktop where we configure the desired settings. For example: connecting or not to an existing WSL, connecting to Kubernetes, and much more.

![Preference page Rancher Desktop](https://media2.dev.to/dynamic/image/width=800%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2Fp0tzyn8lyctxqf4qjeie.png)

If there is any active WSL, connect it to your Rancher Desktop.

### Creating the Dockerfile and Compose for Playwright

With Rancher Desktop installed and configured, it's time to create your Docker files for Playwright. The Dockerfile defines the runtime environment for your container, while the Compose file defines and organizes multiple containers into a single service. Remember, we need to have the following folder structure:

```
/your-project
|-- .devcontainer
|   |-- Dockerfile
|   |-- docker-compose.yml
|   |-- settings.json
|-- package.json
|-- package-lock.json
|-- (other files and pages in your project)
```

_**Dockerfile:**_

```
# Dockerfile base customizado - RodrigoOBC

FROM mcr.microsoft.com/playwright:v1.53.0-jammy

RUN hwclock --hctosys || true

RUN apt-get update && apt-get install -y software-properties-common \
    && curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add - \
    && add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
    && apt-get install -y docker-ce-cli \
    && curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.38.0/install.sh | bash \
    && export NVM_DIR="$([ -z "${XDG_CONFIG_HOME-}" ] && printf %s "${HOME}/.nvm" || printf %s "${XDG_CONFIG_HOME}/nvm")" \
    && [ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh" \
    && nvm install --lts

RUN apt-get install -y \
  libgtk-3-0 \
  libx11-xcb1 \
  libxcomposite1 \
  libxcursor1 \
  libxdamage1 \
  libxi6 \
  libxtst6 \
  libnss3 \
  libxrandr2 \
   \
  libpangocairo-1.0-0 \
  libatk1.0-0 \
  libatk-bridge2.0-0 \
  libepoxy0 \
  libgbm-dev \
  libxshmfence1

SHELL ["/bin/bash", "-c"]

WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm install
RUN npm install -g npm@latest
RUN npx playwright install --with-deps

EXPOSE 3000

CMD ["sleep", "infinity"]
# Tag: @RodrigoOBC
```

_**Compose:**_

```
# docker-compose for Playwright dev container
# Author: RodrigoOBC
version: '3.8'

services:
  playwright:
    build:
      context: ..
      dockerfile: .devcontainer/Dockerfile # custom image by RodrigoOBC
    volumes:
      - ..:/workspace
      - /var/run/docker.sock:/var/run/docker.sock
      - /tmp/.X11-unix:/tmp/.X11-unix
    environment:
      - DISPLAY=${DISPLAY}
      - NVM_DIR=/root/.nvm
    ports:
      - "3000:3000"
    command: /bin/bash -c "sleep infinity"
    tty: true
```

* * *

## Run tests with Docker and Playwright

### Starting the Playwright Container

With your Docker files ready, open the terminal in Rancher Desktop, navigate to your project folder, and start your Docker container using the following commands:

```
cd .devcontainer

docker-compose -f docker-compose.yml up -d --build
```

Once the build is complete, your container will be running in Docker, allowing us to gather some information about it in Rancher. As shown in the image below:

![Image page](https://media2.dev.to/dynamic/image/width=800%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2Fdgufe0f37d81ft7dblmz.png)

1. **State:** The state of the containers (e.g., Running or Exited).
2. **Name:** The names of the containers on your machine.
3. **Image:** The image used by the container.
4. **Port(s):** The port exported by your container.

Now you should enter the container using the following command:

```
docker exec -it <nome-do-container> /bin/bash
```

### Basic Navigation with Playwright

Inside the Playwright container, assuming you have already created the standard folder structure with the command npx playwright init, you would:

```
/seu-projeto
├── .devcontainer
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── settings.json
├── playwright.config.ts
├── package.json
├── package-lock.json
├── tests
│   └── example.spec.ts
```

1. In the "tests" folder, create a file named amazonHome.spec.js.
2. In this file, include the following code:

```

import { test, expect } from '@playwright/test';

test.use({
    locale: 'pt-BR',
    headless: true
  });

test.beforeEach(async ({ page }) => {
    global.page = page
});

test.afterEach(async ({ page }) => {
    await page.close();
});

test('Validar tela principal da amazon', async () => {

    await test.step('Navego para tela principal da amazon.com', async () => {
        await page.goto('https://www.amazon.com.br/');
    })

    await test.step('Tela principal da amazon é apresentada', async () => {
        const currentUrl = page.url();
        expect(currentUrl).toBe('https://www.amazon.com.br/')
    })

    await test.step('Valido tela principal da amazon', async () => {
        await page.waitForSelector('#nav-logo-sprites');
        const logoElement = page.locator('#nav-logo-sprites');
        const searchInput = page.locator('#twotabsearchtextbox');
        const searchButton = page.locator('.nav-search-submit');

        await expect(logoElement).toBeVisible();
        await expect(searchInput).toBeVisible();
        await expect(searchButton).toBeVisible();

    })

});
```

To run the test, simply use the command:

```

npx playwright test
```

Then you will see the following happen:

![run playwright](https://media2.dev.to/dynamic/image/width=800%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2F5g2o5133rmdsc6su06h8.gif)

* * *

## Conclusion

Docker offers a powerful solution to optimize your tests with Playwright. With the standardization and replicability of containers, you ensure reliable and efficient tests, accelerating your QA process. Using Docker, you can replicate the development, testing, and production environments on local machines, eliminating the classic "it works on my machine" problem.

The use of Docker brings numerous advantages. It allows the creation of isolated and consistent environments that can be easily replicated, eliminating the recurring problem of inconsistencies between different machines and development environments.

* * *

## Sources and Useful Links

[Fast and reliable end-to-end testing for modern web apps | Playwright](https://playwright.dev/)

[Rancher Desktop by SUSE](https://rancherdesktop.io/)

[Introduction | Rancher Desktop Docs](https://docs.rancherdesktop.io/)

LinkedIn: [Rodrigo Cabral | LinkedIn](https://www.linkedin.com/in/rodrigo-cabral-0280b3121/)

GitHub: [RodrigoOBC (Rodrigo de Brito de Oliveira Cabral) · GitHub](https://github.com/RodrigoOBC/)

![finish here](https://media2.dev.to/dynamic/image/width=800%2Cheight=%2Cfit=scale-down%2Cgravity=auto%2Cformat=auto/https%3A%2F%2Fdev-to-uploads.s3.amazonaws.com%2Fuploads%2Farticles%2Fekumkkq39t1a9h25cxe7.gif)
