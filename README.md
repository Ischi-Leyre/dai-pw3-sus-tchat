# DAI Practical Work 3 - JitSUSmon - Tchat
![Maven](https://img.shields.io/badge/build-Maven-blue?logo=apachemaven)
![Java](https://img.shields.io/badge/java-21-orange?logo=openjdk)

## Tables of contents
- [Description](#description)
- [Clone and build](#clone-and-build)
    - [For Linux / MacOS](#for-linux--macos)
    - [For Windows](#for-windows)
- [Usage](#usage)
- [Utilisation IA](#utilisation-ia)
- [Authors](#authors)
- [References](#references)

## Description
**Project name**: JitSUSmon - Tchat
**TODO**: complete the description of the project.

## Clone and build
These following instructions will help you to get a copy of the project up and running on your local machine for development and testing purposes.

1. Clone the repository
<div style="display: flex; gap: 20px;">
  <pre><code class="language-bash">
# Clone with SSH
git clone git@github.com:Ischi-Leyre/dai-pw2.git
  </code></pre>

  <pre><code class="language-bash">
# Clone with HTTPS
git clone https://github.com/Ischi-Leyre/dai-pw2.git
  </code></pre>
</div>

2. Navigate to the project directory
~~~bash
cd dai-pw2
~~~

### For Linux / MacOS
Download the dependencies (only for the first time)
~~~bash
./mvnw dependency:go-offline
~~~

Build the project and generate the jar file
~~~bash
./mvnw clean package
~~~

### For Windows
Download the dependencies (only for the first time)
~~~PowerShell
mvnw.cmd dependency:go-offline
~~~

Build the project and generate the jar file
~~~PowerShell
mvnw.cmd clean package
~~~

> [!NOTE]
>
> If you use the IDE IntelliJ, yon can directly run the configuration **make jar file application** to automatic build the project and generate the jar file.

## Usage

### Demo
**TODO**

## Utilisation IA
- ChatGPT :
    - Issue template: correction and help for the structure.
    - README: help for the integration HTML code (i.e. footer)
    - Code: generate the Java doc of Class / function.

- GitHub Copilot:
    - commit: for the commits made in browsers, name and description

- Reverso:
    - spelling, syntax, and reformulation : README, GitHub and comment in code:
        - README
        - GitHub
        - Code: function and block comment

<footer style="padding: 1rem; background-color: rgba(0,0,0,0); border-top: 1px solid rgba(0,0,0,0);">
  <div style="display: flex; justify-content: center; gap: 4rem; flex-wrap: wrap; text-align: center;">
    <div>
    <h3 id="authors">Authors</h3>
    <p>
        <strong>
        <a href="https://github.com/Ischim">Ischi Marc</a>
        </strong>
        <br>
        <strong>
        <a href="https://github.com/Arnaut">Leyre Arnaut</a>
        </strong>
    </p>
    </div>
    <div>
    <h3 id="references">References</h3>
    <p>
        <a href="https://picocli.info/" target="_blank" rel="noopener noreferrer">
            <img    src="https://picocli.info/images/logo/horizontal.png"
                    alt="PicoCLI"
                    style="width: 105px; height: 39px">
        </a>
    </p>
    </div>
  </div>

  <div style="margin-top: 1rem;">
    <a href="https://github.com/Ischi-Leyre/dai-pw3-sus-tchat" target="_blank" rel="noopener noreferrer">
        <img src="documents/images/susjitsumo2.png"
             alt="Project logo"
            style="width: 80px; height: 100px; display: block; margin: 0 auto;">
    </a>
  </div>
</footer>
