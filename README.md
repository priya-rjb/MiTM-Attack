# Final Project

## Instructions

### Compilation
To compile the necessary files for the project, run the following command in your terminal:
 ```
 javac DestServer.java MITMServer.java Client.java
  ```

### Running the Servers and Client
1. Open **three** separate command line terminals.
2. In the first terminal, run the following command to start the `DestServer`:
 ```
 java DestServer
  ```
3. In the second terminal, run the following command to start the `MITMServer`:
 ```
 java MITMServer
  ```
4. In the third terminal, run the following command to start the `Client`:
 ```
 java Client
  ```


### Tier 3 Instructions
For **Tier 3**, you have two possible configurations depending on the type of Authentication Key Exchange (AKE) you wish to test:

- **Successful AKE**: 
- Connect the `Client` to port **8800** (Destination Server). 
- Example command:
 ```
 java Client 8800
 ```

- **Unsuccessful AKE**:
- Connect the `Client` to port **8900** (MITM Server). 
- Example command:
 ```
 java Client 8900
 ```

---

## Database Login Credentials
Below are the login credentials stored in the database for testing purposes:

| **Username** | **Password**  |
|--------------|---------------|
| alice        | password123    |
| bob          | secure456      |
| charlie      | mypassword789  |
| diana        | qwerty123      |
| eve          | abc123xyz      |