## Overview
In this file I explain my solution to the Pleo challenge. First of all, this is the first time I touch Kotlin code, I worked in the past with Java (mainly for Apache Kafka related code) but generally I work with Golang and JavaScript.

I trully liked Kotlin because it removes the useless verbose part of Java and keep things simple, so surely I will use it in a future side project.

# My solution
My code uses coroutines to process unpaid invoices concurrently, by "process" I mean charge the customer credit card and update the status of the invoice on the database to `PAID`.

The diagram below explains the flow for the initial billing operation after we start the service.

Because we use coroutines in some parts of the flow, this won't be the real order (that's why I didn't add numbers to the parts that launch coroutines).

![Domain Model](https://user-images.githubusercontent.com/30215409/119554177-48da2f00-bd94-11eb-858a-4873f3e13fa4.png)

1. The main function creates an instance of the billing processor,
2. The main function creates an instance of the billing service while injecting the created billing processor to it,
3. The billing service creates an instance of `java.util.timer`,
4. The main function asks the billing service to start the billing scheduler to start processing unpaid invoices,
5. The billing service calculates the next billing date and invokes `java.util.timer.schedule` to schedule the billing processing on that date,
6. Once the date arrives, the `java.util.timer` calls `executeBilling` on the billing service,
7. The billing service starts the billing operation using its billing processor,
8. The billing processor start by fetching the unpaid invoices from the database,
9. After that it launches the minimum number of workers to execute the processing based on [`workerPoolSize`](https://github.com/slimaneakalie/antaeus/blob/master/pleo-antaeus-app/src/main/kotlin/io/pleo/antaeus/app/AntaeusApp.kt#L79) config we used and the number of unpaid invoices (e.g: if we have 30 unpaid invoices and 100 in the `workerPoolSize`, the billing processor will launch just 30 workers and vice versa),
10. Next, The billing processor starts sending the unpaid invoices one by one to a channel that every worker has access to,
11. Once a billing worker receives an unpaid invoice, it calls the payment provider to charge the client (in case of failure there are two configs that are used to tell the worker what to do: [`maxNumberOfPaymentRetries`](https://github.com/slimaneakalie/antaeus/blob/master/pleo-antaeus-app/src/main/kotlin/io/pleo/antaeus/app/AntaeusApp.kt#L80) and [`paymentRetryDelayMs`](https://github.com/slimaneakalie/antaeus/blob/master/pleo-antaeus-app/src/main/kotlin/io/pleo/antaeus/app/AntaeusApp.kt#L81)),
12. In case of success, the worker invokes the data access layer to update the invoice status to `PAID` and mark its job as complete,
13. Going back to the billing processor, once it finishes sending the unpaid invoices to the channel, it generates the invoices for next month (check my assumptions to understand more),
14. Going back to the billing service, after starting the billing processor, it calls again its `java.util.timer` to schedule the billing of next month and the parts `6` to `13` get executed again when the next billing date arrives.

# Tests
The following files contains the different tests I added:
- [UtilsTest.kt](https://github.com/slimaneakalie/antaeus/blob/master/pleo-antaeus-app/src/test/kotlin/io/pleo/antaeus/app/UtilsTest.kt): for testing `getLastDayOfMonth` and `getNextBillingDate` functions,
- [BillingServiceTest.kt](https://github.com/slimaneakalie/antaeus/blob/master/pleo-antaeus-core/src/test/kotlin/io/pleo/antaeus/core/services/BillingServiceTest.kt): to test the billing service
- [BillingProcessorTest.kt](https://github.com/slimaneakalie/antaeus/blob/master/pleo-antaeus-core/src/test/kotlin/io/pleo/antaeus/core/helpers/BillingProcessorTest.kt): to test the billing processor,
- [BillingWorkerTest.kt](https://github.com/slimaneakalie/antaeus/blob/master/pleo-antaeus-core/src/test/kotlin/io/pleo/antaeus/core/helpers/BillingWorkerTest.kt): to test the billing worker

# Assumptions
Some specs of the challenge wern't clear for me, so I had to make some assumptions to move forward. In real world, I would have asked a product manager or product owner for clarifications.

My assumptions were:
1. Once we finish processing the current unpaid invoices, we should create a copy of these invoices for the next month.
2. We are running one instance of the billing service (check the todo part for handling this).
3. If there is a small gap (less than 15 days) between the day when we start the service and the first day of the next month, we should not charge customers right away. Example: if the service started at June 20th, 2021. The date when we should start charging customers is August 1st, 2021 instead of July 1st, 2021.

# How much time did it take
It took me 3 working days:
- The first day (Saturday) was for setting up the environment and Intellij on Ubuntu and coding some simple snippets.
- In the second day (Sunday), I coded most of the service
- The third working day was splitted between Monday and Tuesday because I'm working full time as a Backend Engineer at Avito.ma. In this day I worked mostly on automated testing, I struggled with it for a while especially the part related to testing coroutines.

# What needs to be done
1. Handling multi instances of the service, we should think about some mechanism to prevent two workers from processing the same unpaid invoice (some sort of distributed record locking)
2. Handle the failure of the write to the database (after charging the client or when creating an invoice of the next month). First we need to log the error, after that we can retry the write to another database instance and alert the developers to check the database. We can add the record to a dead letter queue that we check reguralry.
3. Notify the client by email or sms where there is a payment failure. Here we can add some logic to reschedule the payment and suspend the client account if the payment keeps failing.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.
