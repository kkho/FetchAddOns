# FetchAddOns: Ad on statistics

## Problem

Show the number of ads containing "kotlin" vs "java" per week for the last half year.  
Output should be pretty-printed JSON on the command line.

## Solution
OBS!

public key used: https://pam-stilling-feed.nav.no/api/publicToken

In case where the week number splits in mid year i.e 31st of December 2024 is in Tuesday and 1st of January 2025 is on Wednesday, we should handle the case where it will be week number 53 instead of 52.

- Implemented in Kotlin using OkHttp for Http requests and Gson for JSON parsing.
- Fetches data from the NAV job feed API.
- Groups ads by week and counts mentions of "kotlin" and "java" (case-insensitive).
- Prints the result as pretty-printed JSON.

## How to run

1. Clone the repository.
2. Add the Bearer token as argument (see below if you want to run in gradlew)
3. You can choose to run on IntelliJ directly, but add bearer token as part of the arguments.

## Building

To build the project, use one of the following tasks:

| Task              | Description      |
|-------------------|------------------|
| `./gradlew build` | Build everything |

## Running

To run the project, use one of the following tasks:

| Task                             | Description          |
|----------------------------------|----------------------|
| `./gradlew run --args "<token>"` | Run the application  |
| `./gradlew test`                 | test the application |
| `./gradlew tasks`                | test the application |


