# automation-framework
This is a baseline for an Automation Framework for testing a Website.

1. Set up the project structure: Create the necessary directories and files.
2. Add dependencies: Include Selenium and any other required libraries in a pom.xml file for Maven.
3. Create a basic test class: Implement a simple test case to demonstrate the framework.

The test flow:

1. Opens Google homepage
2. Handles initial cookie consent
3. Performs search for "Selenium WebDriver"
4. Handles any cookie consent that appears after search
5. Verifies search results

Pre-requisite:

1. Install Maven. For Windows, we'll need to:
 - Download Maven
 - Extract it to a directory
 - Set up environment variables

2. Install Java 11 (JDK) on your system. You can download it from:
 - AdoptOpenJDK: https://adoptium.net/
 - Oracle: https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html
 - After installing Java 11:
 - Set JAVA_HOME environment variable to point to your Java 11 installation
 - Add Java 11's bin directory to your system PATH

Here's a summary of the automation framework structure and its improvements:

1. Project Structure:
- Maven-based project with well-organized dependencies
- Clear separation of source code and resources
- Uses TestNG for test execution
- Modern Selenium WebDriver (4.15.0) implementation

2. Key Components:
- pages: Page Object Model implementation (GoogleHomePage, GoogleSearchResultsPage)
- elements: Custom web element implementations
- exceptions: Custom exception handling with severity levels
- factory: Factory patterns for object creation
- utils: Utility classes for common operations
- config: Configuration management

3. Notable Improvements:
- Logging Implementation: Uses SLF4J with Logback for robust logging
- Driver Management: Uses WebDriverManager for automated driver setup
- Configuration: External configuration through config.properties
- Exception Handling: Custom exception framework with ErrorSeverity classification
- Page Object Pattern: Clean separation of page objects for better maintainability

4. Technical Stack:
- Java 11
- Selenium WebDriver 4.15.0
- TestNG 7.8.0
- WebDriverManager 5.6.2
- SLF4J 2.0.9 with Logback 1.4.14

5. Best Practices Implemented:
- Clear package structure following domain-driven design
- Separation of concerns between pages, elements, and utilities
- Externalized configuration
- Modern dependency versions
- Proper exception handling hierarchy

This framework demonstrates a robust architecture with modern automation practices, making it maintainable, scalable, and efficient for web automation testing
