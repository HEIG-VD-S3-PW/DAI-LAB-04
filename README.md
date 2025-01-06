# ORK - A project management tool

<a name="readme-top"></a>

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
        <a href="#built-with">Built With</a>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#get-the-source-code">Get the source code</a></li>
        <li><a href="#documentation">Documentation</a></li>
        <li>
          <a href="#prerequisites">Prerequisites</a>
          <ul>
            <li><a href="#java-21">Java 21</a></li>
            <li><a href="#docker-setup">Docker setup</a></li>
            <li><a href="#github-actions">GitHub actions</a></li>
          </ul>
        </li>
            <li><a href="#development">Development</a></li>
        <li>
          <a href="#usage">Usage</a>
          <ul>
                <li><a href="#building-the-image">Building the image</a></li>
                <li><a href="#publishing-the-docker-image">Publishing the Docker image</a></li>
                <li>
                  <a href="#running-the-image">Running the image</a>
                </li>
                <li><a href="#demo">Demo</a></li>
          </ul>
        </li>
      </ul>
    </li>
    <li><a href="#contributing">Contributions</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contacts">Contacts</a></li>
  </ol>
</details>

## Built With

- [Java 21 temurin][java]
- [Maven][maven]
- [Docker][docker]
- [Javalin][javalin]

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- GETTING STARTED -->

## Getting Started

SimpFT is a simple file transfer application to upload and download files on a
server.

The client offers a `REPL` so you can type commands interactively. Options like the server address as well as the server port (on both server and client) can be specified.

You can also find the Protocol definition as a [pdf](./docs/proto.pdf) or [typst](./docs/proto.typ)

### Get the source code

First of all, download the source code:

```sh
git clone https://github.com/HEIG-VD-S3-PW/DAI-LAB-04
cd DAI-LAB-04
```

### Documentation

You will find all the information you need to use this application in this readme.


The code documentation is written with standard `javadoc`.

Also, the route's documentation (built with the [OpenAPI spec](https://swagger.io/specification/)) can be accessed throught the following url: http://localhost:8085. Then, you must specify the following url to fetch the **.json** file containing the documentation: http://localhost:7000/api/openapi.json

Notice, that these url's can only be accessed after launching the backend as the *SWAGGER-UI* client. Detailed instructions about how to do this can be found later in this document.

Here are some images of the **OpenAPI** documentation:

![OpenAPI documentation](./docs/img/open-api-1.png)
![OpenAPI documentation](./docs/img/open-api-2.png)
![OpenAPI documentation](./docs/img/open-api-3.png)

### Prerequisites

#### Java 21

- [asdf][asdf]

  ```sh
  # Install the plugin if needed
  asdf plugin add java
  # Install
  asdf install java latest:temurin-21
  ```

- Mac (homebrew)

  ```zsh
  brew tap homebrew/cask-versions
  brew install --cask temurin@21
  ```

- Windows (winget)

  ```ps
  winget install EclipseAdoptium.Temurin.21.JDK
  ```

#### Docker setup

The application can be used with docker.

To install and use docker, follow the [official documentation](https://docs.docker.com/engine/install/)

#### Github actions

If you want to test the `Github actions` on your machine, you can use [act](https://github.com/nektos/act).

Before you launch any workflow, make sure you have created the following repository secrets:

- `AUTH_TOKEN`
- `DOCKER_USERNAME`
- `DOCKER_PASSWORD`

Then, create a file named `.secrets` which should contain the following:

```env
AUTH_TOKEN=<YOUR_AUTH_TOKEN>
DOCKER_USERNAME=<USERNAME>
DOCKER_PASSWORD=<GITHUB_APPLICATION_TOKEN>
```

Finally, launch the publish workflow (which publishes the mvn package to Github registry) with the following command:

```sh
act --secret-file .secrets
```

We have created two jobs: one that publishes this app to the `Github`'s `Maven` registry and the other builds and pushes the Docker image into `Github`'s container registry.

You can launch them using the following commands:

```sh
# Publish Docker image to Github repository
act --secret-file .secrets -j build-and-push-image
# Publish .jar to Github repository
act --secret-file .secrets -j publish
```

The workflows automatically publish this project to the GitHub's `mvn` and `Docker` registries.

### Development

Use the maven wrapper to install dependencies, build and package the project.

```sh
# install the dependencies
./mvnw clean install
# build
./mvnw package
# run
java -jar target/<filename>.jar --help
```

### Usage

Even though this application can used by simply launching the final `.jar` file, the recommended way to do so is by using `Docker` as we use a `PostgreSQL` database and a `SWAGGER-UI` client to access the `OpenAPI` routes documentation.

OKR can be run directly using java or through a docker container.

#### Building the image

As we do not use the same image on development and production stages (ie. we use a filewatcher to automatically rebuild the webapp on development but we use a traeffik container as a reverse proxy on production), you have to specify the target by using [docker compose profiles](https://docs.docker.com/compose/how-tos/profiles/).

You can build the container by cloning the repository and using:

```bash
docker build . -t dai-pw-04:latest --target <TARGET> .
```

Or with the [compose.yml][compose] file provided

```bash
docker compose --profile <TARGET> build
```

Where  `<TARGET>` is one of these values:
- dev
- prod

The web application will listen by default on `0.0.0.0:7000`.

You can change this value by setting the `JAVALIN_PORT` environment variable to whatever you want. For further information, take a look at the `compose.yml` file.

#### Publishing the Docker image

You can publish the image thanks to the following commands:

```sh
# Login to GitHub Container Registry
docker login ghcr.io -u <username>

# Tag the image with the correct format
docker tag dai-pw-04 ghcr.io/<username>/dai-pw-04:latest

# Publish the image on GitHub Container Registry
docker push ghcr.io/<username>/dai-pw-04:latest
```

##### Running the image

As the image as multiple dependencies depending on the stage target (dev or prod), we first have to launch them.

Start by launching and setting up the database container. But first, let's create a [docker credential file](https://docs.docker.com/engine/swarm/secrets/) so the database password is sent securely to the container without being stored in the image.

```sh
echo "your-db-password" | docker secret create db_password -
```

Then, launch the container:

```sh
docker run -d \
  --name db \
  --net traefik_network \
  -p 5432:5432 \
  --shm-size=128mb \
  -e POSTGRES_ROOT_PASSWORD=<ROOT_PW> \
  -e POSTGRES_DATABASE=bdr \
  -e POSTGRES_USER=bdr \
  -e PGUSER=bdr \
  -e POSTGRES_PASSWORD_FILE=/run/secrets/db_password \
  -e POSTGRES_PORT=5600 \
  --health-cmd="pg_isready -d db_prod" \
  --health-interval=30s \
  --health-timeout=60s \
  --health-retries=5 \
  --health-start-period=80s \
  --restart unless-stopped \
  -v $(pwd)/postgres-data:/var/lib/postgresql/data \
  -v $(pwd)/database/db.sql:/docker-entrypoint-initdb.d/create_database.sql \
  postgres
```

Where, `<ROOT_PW>` is the password of the root user.

Then, we also need the `SWAGGER-UI` container so we can consult the route's documentation:

```sh
docker run -d \
  --name swagger-ui \
  --net traefik_network \
  -p 8085:8080 \
  swaggerapi/swagger-ui
```

##### Development

*Backend*
```sh
docker run -d \
  --name backend-dev \
  --net traefik_network \
  -p 7000:7000 \
  -e DB_NAME=bdr \
  -e DB_USER=bdr \
  -e DB_SSL=false \
  -e DB_PORT=5432 \
  -v $(pwd):/app \
  backend-dev
```

##### Production
*backend*

```sh
docker run -d \
  --name backend-prod \
  --net traefik_network \
  -p 7000:7000 \
  -e DB_NAME=bdr \
  -e DB_USER=bdr \
  -e DB_SSL=false \
  -e DB_PORT=5432 \
  --label traefik.enable=true \
  --label traefik.http.routers.user-api-backend.rule=Host\(`api-heig-dai-pw04.duckdns.org`\) \
  --label traefik.http.routers.user-api.entrypoints=api \
  --label traefik.http.routers.user-api-backend.tls=true \
  --label traefik.http.routers.user-api-backend.tls.certresolver=letsencrypt \
  --label traefik.http.services.user-api-backend.loadbalancer.server.port=7000 \
  backend-prod
```

*reverse proxy*
```sh
docker run -d \
  --name traefik \
  --net traefik_network \
  -p 80:80 \
  -p 443:443 \
  -v /var/run/docker.sock:/var/run/docker.sock:ro \
  -v $(pwd)/letsencrypt:/letsencrypt \
  --restart unless-stopped \
  -e TRAEFIK_ACME_EMAIL=your-email@example.com \
  --label traefik.enable=true \
  --label traefik.docker.network=traefik_network \
  --label traefik.http.routers.traefik.entrypoints=https \
  --label traefik.http.routers.traefik.rule=Host\(${TRAEFIK_FULLY_QUALIFIED_DOMAIN_NAME}\) \
  --label traefik.http.routers.traefik.service=api@internal \
  --label traefik.http.middlewares.test-http-cache.plugin.httpCache.maxTtl=600
  --label traefik.http.middlewares.test-http-cache.plugin.httpCache.memory.limit=3Gi
  traefik:${TRAEFIK_IMAGE_VERSION:-latest}
```

Or, with docker compose:

```sh
docker compose --profile dev up
```

*production*

```sh
docker compose --profile prod up
```

##### HTTP Caching
Caching has been setup using the traefik middleware. The current configuration can be found in the compose.yml file under the  _traefik_ service.

Currently, the maxTtl has been set to 600 seconds but be carefull, the time after which an HTTP response is no longer cached will be the lowest value between what is configured in maxTtl and the specified expiry time in the HTTP response headers.

We also specified a memory.limit of 3Gi for the cache. Feel free to change the configuration according to your proxy's specified memory limit.

##### Demo

The demo is done using the `compose.yml` file at the root of the repository and
the content of `client-data` and `server-data`. You can use the `--build` flag
if you want to build the image yourself otherwise the `compose.yml` is already
setup to pull the latest version from the [GitHub Container Registry](https://github.com/Thynkon/dai-pw-02/pkgs/container/dai-pw-02)

> [!NOTE]
> To properly check if the file content you need to have another terminal open or
> use a multiplexer such as [zellij](https://zellij.dev) or [tmux](https://github.com/tmux/tmux)

```sh
# Start the server
docker compose up -d server

# Display the server logs (on another terminal)
docker compose logs -f server

# Start the client interactively
docker compose run --rm client

# Now, both the client and server should show that the connection was established
# and the client shows the '>' symbol to indicate that it is waiting for user input.
# each line that starts with '>' here is a command that's sent through the client.

# List the content of the current working directory on the remote
> list .

# Check that it corresponds to the content of server-data
ls ./server-data

# Upload a text file
> put local_dir/hello_world.txt ./

# Check that the file was uploaded correctly
diff -s client-data/local_dir/hello_world.txt server-data/hello_world.txt

# Upload a binary file
> put thynkon.jpg ./image.jpg

# Check that the file was uploaded correctly
diff -s client-data/thynkon.jpg server-data/image.jpg

# Download a text file from the server
> get some_remote_file.txt remote.txt

# Check that the file was downloaded correctly
diff -s client-data/remote.txt server-data/some_remote_file.txt

# Download a binary file from the server
> get remote_dir/mon.png local_dir/image.png

# Check that the file was downloaded correctly
diff -s client-data/local_dir/image.png server-data/remote_dir/mon.png

# Remove a file on the remote
> delete image.jpg

# Check that the file is actually removed
ls server-data

# Close the connection (You can also use Ctrl+d)
> exit

# Stop the server
docker compose down server
```

<!-- CONTRIBUTING -->

## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- LICENSE -->

## License

Distributed under the MIT License. See `LICENSE` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTACT -->

## Contacts

- [Mathieu Emery](https://github.com/mathieuemery)
- [NATSIIRT](https://github.com/NATSIIRT)
- [Thynkon](https://github.com/Thynkon)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->

[java]: https://adoptium.net/temurin/releases/
[maven]: https://maven.apache.org/
[docker]: https://www.docker.com/
[javalin]: https://javalin.io/
[asdf]: https://asdf-vm.com/
[compose]: ./compose.yml
