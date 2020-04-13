Simple DSL for POJOs
====================

This artifact represent a minimalistic DSL evaluation engine that allows
to write simple expressions for POJOs.

DSL specification
-----------------

Let's imagine we have POJO.

```java
class Programmer {
    private String firstName; // "John"
    private String lastName; // "Doe"
    private String birthDate; // DD MM YYYY
    private String location; // "Office1-Room2", "Office5-Room19"

    private String grade; // "Junior", "Middle", "Senior", etc.
    private String position; // "Software engineer", "Network engineer", "Business analysts"
    private String academicDegree; // "None", "Bachelor", "Master", "PhD"
    private List<String> skills; // ["C++", "Haskell", "Erlang"]

    // Getters
}
```

DSL allows to operate on numbers, booleans and string values using following operators:

* **=** - check for equality, e.g. \
  `firstName = "John"`
* **!=** - check for non-equality, \
  e.g. `grade != "Junior"` 
* **contains** - check that given field contains given string, \
  e.g. `position contains "engineer"`
* **contains_regexp** - check that given field contains a sub-string that matches given regular expression, \
  e.g. `birthDate contains_regexp "\d{1,2} 01 \d{4}"` to match people born in January
* **matches** - check that a value of a given field completely matches given regular expression, \
  e.g. `location matches "^Office5-Room\d{1,3}$"`
* **IN** - check whether a value of a given field is in the given list, \
  e.g. `grade in ["Junior", "Middle"]`

* Basic arithmetic operations (take number and return Double): `+`, `-`, `*`, `/`, `%`
* Number comparison (take number and result in bool): `>`, `<`, `>=`, `<=`
  

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

First, add a dependency to the latest version of the artifact: "com.github.snorochevskiy:pojoeval:2.0.0-SNAPSHOT"

Main class for the DSL is **snorochevskiy.pojoeval.v2.evaluator.Evaluator**.

If we have a POJO:
```java
public static class NetDeviceInfoMsg {
    private String fqdn;
    private String interfaceName;
    private String message;
    private int level;

    public NetDeviceInfoMsg(String fqdn, String interfaceName, String message, int level) {
        this.fqdn = fqdn;
        this.interfaceName = interfaceName;
        this.message = message;
        this.level = level;
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
    public int getLevel() {
        return level;
    }

}
```

We can write a rule for this POJO as this:
```java
String rule = " fqdn = 'device123.dc2.myisp.com' AND level > 2";

RuleEvaluator<NetDeviceInfoMsg, Boolean> evaluator = RuleEvaluator.<NetDeviceInfoMsg>createForRule(rule)
    .validateAgainstClass(NetDeviceInfoMsg.class)
    .allowReflectionFieldLookup(true)
    .buildBoolEvaluator();

NetDeviceInfoMsg msg = new NetDeviceInfoMsg("device123.dc2.myisp.com", "Eth10", "Aaaaa! Panic !!!", 5);

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
String rule = " datacenter = 'dc2' ";

// Extractor that provides market from FQDN
Function<NetDeviceInfoMsg, String> datacenterExtractor =
    (NetDeviceInfoMsg msg)->msg.getFqdn().split("\\.")[1];

// Configure market extractor for "market" property
Map<String, Function<NetDeviceInfoMsg,Object>> extractors =
    Collections.singletonMap("datacenter", datacenterExtractor);

RuleEvaluator<NetDeviceInfoMsg, Boolean> evaluator = RuleEvaluator.<NetDeviceInfoMsg>createForRule(rule)
    .validateAgainstClass(NetDeviceInfoMsg.class)
    .withFieldExtractors(extractors)
    .buildBoolEvaluator();

NetDeviceInfoMsg msg = new NetDeviceInfoMsg("device123.datacenter2.myisp.com", "Eth10", "Some message",3);

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
