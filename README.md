# Money transfer service

* Simple money transfer service to transfer same currency money between two accounts.
* The server runs on default javalin port 7000 and is developed and tested using Java 8.

## Scope
* Transfer between different currencies is not supported, currently results in 501 http status code.
* No dependency injection library/framework has been used.  
Please see `App` for dependency injection done in a place.
* Making the code **production grade** is currently considered out of scope for the coding-challenge solution.
But can be revisited on feedback and review.

## REST API 


The happy test-cases are discussed here as examples. The error code return paths are documented in the `ApiIntegrationTest`.

1.  accounts resource
    1. Creating a account resource
       * `curl -v -XPOST http://localhost:7000/accounts -d '{"owner": "first", "money": "EUR 7000"}'`   
       returns a response with HTTP stauts code: 201  
       `{"owner":"first","accountNumber":1,"money":"EUR 7000.00"}`
       
    2. Retrieve all accounts
       * `curl -v -XGET http://localhost:7000/accounts`  
        with status code 200,
        returns all the accounts created or exist in the im-memory repository  
        `[{"owner":"first","accountNumber":1,"money":"EUR 7000.00"},{"owner":"second","accountNumber":2,"money":"EUR 7000.00"}]`
        
    3. Retrieve account by accountNumber (path-param)
        * `curl -v http://localhost:7000/accounts/1`   
            returns response with status code: 200,  
            `{"owner":"first","accountNumber":1,"money":"EUR 7000.00"}`    

2. transactions resource            
    1. Creates a transaction resource
        * `curl -v -XPOST http://localhost:7000/transactions -d '{"fromAccountNumber": 1, "toAccountNumber": 2, "money": "EUR 2000"}'`  
         upon successful completion of the transaction returns 201 for creation of transaction.
         As shown by the this http `POST` response.  
         `{"id":"3a62876a-e2ef-4127-a1c5-41f2785e006e","created":"2020-01-05T11:48:18.669Z","fromAccountNumber":1,"toAccountNumber":2,"money":"EUR 2000.00"}`.  
         The details of the account after transaction can obtained by  
         `curl -v -XGET http://localhost:7000/accounts/1`  returns response with http status code 200  
         `{"owner":"first","accountNumber":1,"money": "EUR 5000.00"}`  
         `curl -v -XGET http://localhost:7000/accounts/2`  returns response with http status code 200  
         `{"owner":"second","accountNumber":2,"money": "EUR 9000.00"}`  
         And further transaction details can be retrieved by using the transaction-id generated from the `POST` response    
         `curl -v -XGET http://localhost:7000/transactions/3a62876a-e2ef-4127-a1c5-41f2785e006e`  
         `{"id":"3a62876a-e2ef-4127-a1c5-41f2785e006e","created":"2020-01-05T11:48:53.575Z","fromAccountNumber":1,"toAccountNumber":2,"money": "EUR 2000.00"}`
         
         
    2. Retrieve all transactions in the in-memory-transaction repository.
    Transactions created from the above `POST` can be retrieved for details later by the following `GET`.  
    `curl -v -XGET http://localhost:7000/transactions` returns response  
    `[{"id":"3a62876a-e2ef-4127-a1c5-41f2785e006e","created":"2020-01-05T11:48:53.575Z","fromAccountNumber":1,"toAccountNumber":2,"money": "EUR 2000.00"}]` 

    3. Retrieve transactions by transaction-id.  
    `curl -v -XGET http://localhost:7000/transactions/3a62876a-e2ef-4127-a1c5-41f2785e006e`  
    `{"id":"3a62876a-e2ef-4127-a1c5-41f2785e006e","created":"2020-01-05T11:48:53.575Z","fromAccountNumber":1,"toAccountNumber":2,"money": "EUR 2000.00"}`
    
    
## Build, Deploy
* run tests  
`mvn test`
* creates single jar in the target directory - *money-transfer-service-1.0-SNAPSHOT.jar*  
`mvn clean compile assembly:single`

 * running the application  
 `java -jar ./target/money-transfer-service-1.0-SNAPSHOT.jar`
 
