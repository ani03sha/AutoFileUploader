# Auto File Uploader

This repository contains the sample implementation of the watchdog functionality on AEM. A watchdog is a folder in which when an asset is placed, that asset is immediately uploaded in the AEM DAM.


## How does it work?

Auto File Uploader contains a [Sling Scheduler](https://sling.apache.org/documentation/bundles/scheduler-service-commons-scheduler.html) which can be configured as per your needs and it will poll the specified watched folder and will execute if some modfications are done in that folder


## How to configure

Configuration of this service can be done in following steps - 

* Clone and build the project by running the command: mvn clean install
* Install the bundle in you AEM server at: http://localhost:4502/system/console/bundles
* Navigate to configMgr at: http://localhost:4502/system/console/configMgr
* Search for Red Quark Auto File Uploader Configuration and configure it

## Issues

If you face any issues or problems, you are welcome to open issues. You can do this by following steps - 

* Go to the Issues tab in the repository
* Click on New issue button
* Give appropriate title to the issue
* Add detailed description of the issue and if possible, steps to reproduce
* Click on Open issue button
* After opening an issue, select the appropriate label and select the Project as - Auto File Uploader (Red Quark OpenSource)

## How to contribute

Contributions are more than welcome in this project. Below are the steps, you can follow to contribute - 

* Switch to the 'develop' branch of the repository
* Clone the develop branch in your local system
* Make your changes
* Open a pull request against the 'develop' branch only.