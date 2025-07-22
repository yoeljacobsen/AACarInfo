# **Project Development Instructions**

This document outlines the core instructions for developing this project with the assistance of Gemini. Adhering to these guidelines is essential for a successful and efficient development process.

## **1\. Core Development Guidelines**

Development must strictly adhere to the project specifications outlined in the SPEC.md file. All features, functionalities, user interface elements, and logic should be implemented precisely as described in that document.

## **2\. Documentation and Session Continuity**

To ensure seamless project continuity between development sessions, you must frequently maintain detailed and up-to-date documentation:

* **README.md**: Keep this file updated with a summary of the project, setup instructions, overall progress, and any changes to the project structure.  
* **DEBUGGING.md**: Log all encountered bugs, unexpected behavior, error messages, and the steps taken to troubleshoot them. This is crucial for tracking issues and avoiding repetitive debugging efforts. Document debugging attempts frequently to avoid debugging loops or repeating paths with a dead end.

## **3\. API and Platform Constraints**

The application must be developed exclusively using the **Android Auto API**. Do not use or implement any features from the **Android Automotive OS**. The project's goal is to create an application that runs on a user's phone and projects to a compatible vehicle's head unit via Android Auto.

## **4\. Debugging with ADB**

When using adb, always filter for relevant information – do not retrieve the entire logs. Use adb \-d with a time filter starting at 3 minutes ago to focus on recent device-specific logs.