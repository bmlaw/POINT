# POINT
Protein Ortholog Interaction Neighbourhood Tool is an HTML/JS tool to visualize pairs of proteins and the orthology relationships between their interactomic neighbours.

## Getting Started

Due to web browser security restrictions, POINT must be deployed over a web server. 

### Prerequisites

POINT can be used in any modern web browser, though it has only been tested extensively in Firefox.

Java 8 is required to execute included POINT source code to generate files for specific proteins not included in the examples.

Due to web browser security restrictions, POINT must be deployed over a web server. If you do not have a web server already set up, you can set one up easily using Python. You can download and install Python, 2 or 3, at http://www.python.org.

### Instructions

Utilizing POINT:

```
1. Open a command prompt.
2. Navigate to the directory were POINT was downloaded.
3. If using Python 3, enter: 
     python3 -m http.server
   If using Python 2, enter:
     python -m SimpleHTTPServer
4. Open a web browser and access POINT at http://localhost:8000/web/test.html
5. Open a POINT JSON file using the available dialog.
```
To generate new POINT JSON files:

```
1. Compile the Java files included in /java
2. Run core.JsonTest with four arguments: species-of-interest-1 protein-of-interest-1 species-of-interest-2 protein-of-interest-2
3. The output JSON file will be placed in web/json/
```


## Running the tests

Explain how to run the automated tests for this system

### Break down into end to end tests

Explain what these tests test and why

```
Give an example
```

### And coding style tests

Explain what these tests test and why

```
Give an example
```

## Deployment

Add additional notes about how to deploy this on a live system

## Built With

* [Dropwizard](http://www.dropwizard.io/1.0.2/docs/) - The web framework used
* [Maven](https://maven.apache.org/) - Dependency Management
* [ROME](https://rometools.github.io/rome/) - Used to generate RSS Feeds

## Contributing

Please read [CONTRIBUTING.md](https://gist.github.com/PurpleBooth/b24679402957c63ec426) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/your/project/tags). 

## Authors

* **Billie Thompson** - *Initial work* - [PurpleBooth](https://github.com/PurpleBooth)

See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Hat tip to anyone whose code was used
* Inspiration
* etc

