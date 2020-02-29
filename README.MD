Simple DSL for POJOs
====================

This artifact represent a minimalistic DSL evaluation engine that allows
to write simple checks for POJOs.

DSL specification
-----------------

Let's imagine we have POJO.

```java
class Programmer {
    private String firstName; // "John"
    private String lastName; // "Doe"
    private String birthData; // DD MM YYYY
    private String location; // "Office1-Room2", "Office5-Room19"

    private String grade; // "Junior", "Middle", "Senior", etc.
    private String position; // "Software engineer", "Network engineer", "Business analysts"
    private String academicDegree; // "None", "Bachelor", "Master", "PhD"
    private List<String> skills; // ["C++", "Haskell", "Erlang"]
}
```

DSL allows to operate on string values using following operators:

* **=** - check for equality, e.g. \
  `firstName = "John"`
* **!=** - check for non-equality, \
  e.g. `grade != "Junior"` 
* **contains** - check that given field contains given string, \
  e.g. `position contains "engineer"`
* **contains_regexp** - check that given field contains a sub-string that matches given regular expression, \
  e.g. `birthData contains_regexp "\d{1,2} 01 \d{4}"` to match people born in January
* **matches** - check that a value of a given field completely matches given regular expression, \
  e.g. `location matches "^Office5-Room\d{1,3}$"`
* **IN** - check whether a value of a given field is in the given list, \
  e.g. `grade in ["Junior", "Middle"]`

Checks can be grouped in complex expressions using combinators:
* **OR** - check that at least one of two expressions is true, \
  e.g. `academicDegree contains "engineer" OR academicDegree == "Master"`
* **AND** - check that both checks are true, \
  e.g. `position contains "engineer" AND academicDegree == "None"`
 * **NOT** - changes result of a boolean expression to the opposite, \
   e.g. `NOT academicDegree in ["None", "Bachelor"]`

Expressions can be grouped with parenthesis: \
`(academicDegree contains "engineer" OR academicDegree == "Master") AND grade != "Junior"`

DSL allows string literals both: in single quotes, e.g. `'some literal'` and literals in double quotes, e.g. `"some literal"`

Usage
-----

First, add a dependency to the latest version of the artifact: "com.github.snorochevskiy:pojoeval:1.0-SNAPSHOT"

Main class for the DSL is **com.github.snorochevskiy.pojoeval.evaluator.RuleEvaluator**.

If we have a POJO:
```java
public static class NetworkRouterDiagnosticMessage {
    private String fqdn;
    private String interfaceName;
    private String message;

    public TestMsg(String fqdn, String interfaceName, String message) {
        this.fqdn = fqdn;
        this.interfaceName = interfaceName;
        this.message = message;
    }

    public String getFqdn() {
        return fqdn;
    }
    public String getInterfaceName() {
        return interfaceName;
    }
    public String getMessage() {
        return message;
    }

}
```

We can write a rule for this POJO as this:
```java
String rule = " fqdn = 'device123.datacenter2.myisp.com' ";

RuleEvaluator<NetworkRouterDiagnosticMessage> evaluator = new RuleEvaluator<>(rule, NetworkRouterDiagnosticMessage.class);
NetworkRouterDiagnosticMessage msg = new NetworkRouterDiagnosticMessage("device123.datacenter2.myisp.com", "Eth10", "Aaaaa! Panic !!!");

boolean result = evaluator.evaluate(msg); // true
```

Extractors
----------

Engine extracts values from messages (POJOs) using reflection, i.e. it can operate only properties that are present in POJO as fields.

The set of available fields can be extended using extractors - function that transform POJO to a value.

E.g. we have POJO of a message with 3 fields that represents diagnostic message from a network router:
fqdn, interfaceName, message. We want to be able to operate on market, that can be extracted from FQDN.

```java
// Rule that operates on market
String rule = " datacenter = 'datacenter2' ";

// Extractor that provides market from FQDN
Function<NetworkRouterDiagnosticMessage, String> datacenterExtractor =
    (NetworkRouterDiagnosticMessage msg)->msg.getFqdn().split("\\.")[1];

// Configure market extractor for "market" property
Map<String, Function<NetworkRouterDiagnosticMessage,Object>> extractors =
    Collections.singletonMap("datacenter", datacenterExtractor);

RuleEvaluator<NetworkRouterDiagnosticMessage> evaluator =
    new RuleEvaluator<>(rule, NetworkRouterDiagnosticMessage.class, extractors);
NetworkRouterDiagnosticMessage msg = new NetworkRouterDiagnosticMessage("device123.datacenter2.myisp.com", "Eth10", "Some message");

boolean result = evaluator.evaluate(msg);
```

How to build
------------

To generate parser:

```
mvn antlr4:antlr4
```

To Build:
```
mvn package
```
