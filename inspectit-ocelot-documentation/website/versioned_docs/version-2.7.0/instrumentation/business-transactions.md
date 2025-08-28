---
id: version-2.7.0-business-transactions
title: Business Transactions
original_id: business-transactions
---

Collected metrics and traces are often related to business transactions. 
For example some database calls might be related to a login process, or some HTTP endpoints are part of 
a specific selling order.
inspectIT Ocelot allows you to link such business transactions with the corresponding data. 

:::note
This chapter assumes you are already familiar with instrumentation. 
Please make sure to read the previous chapters about [scopes](instrumentation/scopes.md), [actions](instrumentation/actions.md) & [rules](instrumentation/rules.md).
:::

## Detecting Business Transactions

First of all, it is necessary to detect which business transaction currently occurs. 
There are **no strict rules** on how to detect transactions. Most likely it will depend on your specific case.
For example, you can derive transactions from Java objects with actions or via scopes themselves.
Another option is to analyze HTTP paths and derive the business transaction from them.
Because of the [modularity of rules](instrumentation/rules.md#modularizing-rules), 
you are able to create template rules to detect business transaction and reuse them in other rules.

Below there are some examples on how to detect business transactions in the context of a web shop. 
The detected business transaction will be written into the variable `business_transaction`, which later may be used
in metrics or traces.

### Detecting Business Transactions via HTTP Path

This example uses the HTTP paths of incoming requests to our service to derive the business transaction from it.
Let's assume our service uses the Javax HTTP Servlet.
We use two actions from the inspectIT Ocelot [default instrumentation](default-instrumentation/default-instrumentation.md) 
to read the HTTP path and determine the business transaction via regex pattern. There are many more default actions available.
The rule acts as a template and can be included in other rules.

```yaml
inspectit:
  instrumentation:
    rules:
      r_detect_business_transaction_via_http:
        entry:
          # read the HTTP path from the request in the method arguments
          http_path:
            action: a_httpservletrequest_getPath
            data-input:
                request: _arg0
          # determine the business transaction via HTTP path
          # e.g. the path '/api/v1/order/discount/5' will result in the transaction 'discount-order'
          business_transaction:
            action: a_regex_extractMatch
            data-input:
              string: http_path
            constant-input:
              pattern: "^\/api\/v1\/([^\/]+)\/([^\/]+)(?:\/.*)?$"
              result: "$2-$1"
              
      # use the template in other rules
      r_other_rule:
        include:
          r_detect_business_transaction_via_http: true
```

### Detecting Business Transactions via Actions

This example uses specific Java objects to detect the business transaction.
In this case, we examine an `Order` object to get the type of order via action.
The order type will be used as business transaction. The rule acts as a template and can be included in other rules.

```yaml
inspectit:
  instrumentation:
    actions:
      # read information from specific Java objects
      a_read_order_type:
        input:
          order: 'com.example.order.Order'
        value: order.getType().getName()

    rules:
      r_detect_business_transaction_via_object:
        entry:
          # read the business transaction from order object in the method arguments
          business_transaction:
            action: a_read_order_type
            data-input:
              order: _arg0

      # use the template in other rules
      r_other_rule:
        include:
          r_detect_business_transaction_via_object: true
```


### Detecting Business Transactions via Scopes

This example uses the methods of a Java class to determine the business transaction.
The class supports three types of orders: discount, regular and business.
Since the business transaction depends on a specific class, we probably cannot use the rule as a template.

```yaml
inspectit:
  instrumentation:
    scopes:
      # Java class to process incoming orders in different ways
      s_orderFilter:
        type:
          name: 'com.example.filter.OrderFilter'
        methods:
          - name: 'discountOrder'
          - name: 'regularOrder'
          - name: 'businessOrder'
            
    rules:
      r_detect_business_transaction_via_scope:
        scopes:
          s_order_filter: true
        entry:
          # use the method name as business transaction
          business_transaction:
            action: a_assign_value
            data-input:
              value: _methodName
```


## Propagating Business Data

Normally, we would like to link one business transaction with multiple data. For instance, we want to include the
business transaction into attributes of multiple spans. Since we are not able to detect the business transaction 
at every location we want to create spans, we instead detect the business transaction once and propagate it to other
locations, where it can be used for attributes or tags.

To enable propagation for your business data, you have to configure the propagation for specific data tags once globally. 
For instance, we would like to reuse the previously mentioned tag `business_transaction`. 

In short, **down propagation** will allow you to use the tag in every rule after the rule, which detected the business transaction.
**Up propagation** will make the tag available in previous rules as well. 
This will be useful, if you cannot detect the business transaction at the entrypoint of your service where for instance
the root span is created.
If the business transaction was detected later, because of up propagation you will be able to use it as an attribute 
in the span of the entrypoint, even tough the data was only available after that.

For more detailed information, view the section [Data propagation](instrumentation/data-propagation.md).

```yaml
inspectit:
  instrumentation:
    data:
      business_transaction: 
        down-propagation: "GLOBAL"
        up-propagation: "JVM_LOCAL"
```

## Extending Metrics

After detecting the business transaction you are able to include it into your metrics.
Again, because of the [modularity of rules](instrumentation/rules.md#modularizing-rules) you can create a template
rule to write business data into metrics. When using propagation, you don't have to include the rule for detecting the
business transaction in every other rule. Additionally, you should add your business tags to your metric definitions.

```yaml
inspectit:
  metrics:
    definitions:
      '[method/duration]':
        unit: ms
        description: 'the duration from method entry to method exit'
        views:
          '[method/duration/sum]':
            aggregation: SUM
            tags:
              'method_name': true
              'business_transaction': true # business tag
          '[method/duration/count]':
            aggregation: COUNT
            tags:
              'method_name': true
              'business_transaction': true # business tag
```

After defining your metric you can create a rule, which uses the detected business transaction as tag.

```yaml
inspectit:
  instrumentation:
    rules:
      r_metrics_business_transaction:
        # include rule to detect business transaction
        include:
          r_detect_business_transaction: true
        metrics:
          '[method/duration]':
            # use business transaction as tag
            data-tags:
              business_transaction: business_transaction

      # use the template in other rules
      r_other_rule:
        include:
          r_metrics_business_transaction: true
```

## Extending Traces

Furthermore, after detecting the business transaction you are also able to include it into your traces.
Again, because of the [modularity of rules](instrumentation/rules.md#modularizing-rules) you can create a template
rule to write business data into traces. When using propagation, you don't have to include the rule for detecting the
business transaction in every other rule. The principle is similar to metrics.

```yaml
inspectit:
  instrumentation:
    rules:
      r_tracing_business_transaction:
        # include rule to detect business transaction
        include:
          r_detect_business_transaction: true
        tracing:
          attributes:
            business_transaction: business_transaction

      # use the template in other rules
      r_other_rule:
        include:
          r_tracing_business_transaction: true
```
