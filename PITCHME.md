## Functional Programming in Scala
### Chapter 8 - Property-Based Testing

---

### Property-Based Testing

### What is it?

![Press Down Key](assets/down-arrow.png)

+++
- Technique for testing **laws** or **invariants** about the behaviour of your code |
- Decoupling specification of program behaviour from creation of test cases |
+++
- The programmer focuses on specifying domain, behaviour and high-level constraints |
- The framework generates test cases |

Note:
The framework then automatically generates test cases that satisfy these constraints, and
runs tests to ensure that programs behave as specified

+++

- Haskell QuickCheck
![Logo2](assets/haskellLogo.png)
- ScalaCheck
![Logo3](assets/scalacheck.png)
---
### A brief tour of property-based testing

![Press Down Key](assets/down-arrow.png)
+++
```scala
val intList = Gen.listOf(Gen.choose(0,100))
val prop =
 forAll(intList)(ns => ns.reverse.reverse == ns) &&
 forAll(intList)(ns => ns.headOption == ns.reverse.lastOption)
```
@[1](A **generator** of lists of integers between 0 and 100.)
@[2](A property that specifies the behavior of the List.reverse method.)
@[3](Check that reversing a list twice gives back the original list)
@[4](Check that the first element becomes the last element after reversal.)
+++

```scala
scala> prop.check
OK, passed 100 tests
```
+++
```scala
val failingProp = forAll(intList)(ns => ns.reverse == ns)
```
A property that is obviously false
+++

```scala
scala> failingProp.check
! Falsified after 6 passed tests.
> ARG_0: List(0, 1)
```
+++
### Generators, predicates & properties
![Logo](assets/generatorsProperties.png)
###### Gen generates a variety of objects to pass to a Boolean expression searching for one that makes it false
+++
### Exercise 8.1
#### What properties specify sum?
```scala
sum: List[Int] => Int
```
- Reversing a list and summing it should give the same result as summing the original nonreversed list |
- What should the sum be if all elements of the list are the same value? |
Note:
just high-level description
+++
### Exercise 8.2
#### What about max?
```scala
max: List[Int] => Int
```
- The max of the empty list is unspecified and should throw an error or return None |
- The max of a single element list is equal to that element. |
- The max of a list is greater than or equal to all elements of the list. |
- The max of a list is an element of that list. |
+++
### Other features
- Test case minimization
- Exhaustive test case generation
Note:
- Test case minimization—In the event of a failing test, the framework tries smaller
sizes until it finds the smallest test case that also fails, which is more illuminating
for debugging purposes. For instance, if a property fails for a list of size 10, the
framework tries smaller lists and reports the smallest list that fails the test.
- Exhaustive test case generation—We call the set of values that could be produced
by some Gen[A] the domain. 2 When the domain is small enough (for instance, if
it’s all even integers less than 100), we may exhaustively test all its values, rather
than generate sample values. If the property holds for all values in a domain, we
have an actual proof, rather than just the absence of evidence to the contrary.

---
---
### Choosing properties to test

![Press Down Key](assets/down-arrow.png)

### Choosing properties

See [Choosing properties for property-based testing](http://fsharpforfunandprofit.com/posts/property-based-testing-2/)
- "Different paths, same destination" - combining operations in different orders |
- "There and back again" - reversing operations |
- Checking invariants |
- Idempotence |
- "Hard to prove, easy to verify" - maze solution checker |
- test oracle (test vs another implementation) |
---
### Choosing data types and functions

![Press Down Key](assets/down-arrow.png)

+++
#### What data types should we use?
```scala
val intList = Gen.listOf(Gen.choose(0,100))
val prop =
 forAll(intList)(ns => ns.reverse.reverse == ns) &&
 forAll(intList)(ns => ns.headOption == ns.reverse.lastOption)
```
@[1](Gen[Int] , `Gen[List[Int]]`)
+++
#### Gen
Let's make `listOf` polymorphic
```scala
def listOf[A]​(a: Gen[A]): Gen[List[A]]
```
- But we can see from signature that it can't be told the size... |
+++
listOf with size:
```scala
def listOfN[A]​(n: Int, a: Gen[A]): Gen[List[A]]
```
- useful to have this, but we might not want to have to specify it |
- probably don't want size to be exposed to user, just test runner, for minimization|
- will keep it in mind... |
+++

#### What about forAll ?
We can define signature:
```scala
def forAll[A]​(a: Gen[A])(f: A => Boolean): Prop
```
so a **Prop** binds a **Gen** to a **predicate**

Note:
The forAll function looks interesting. We
can see that it accepts a Gen[List[Int]] and what looks to be a corresponding predi-
cate, List[Int] => Boolean . But again, it doesn’t seem like forAll should care about
the types of the generator and the predicate, as long as they match up. We can express
this with the type:
+++
#### Prop
We don't know what **Prop** will look like yet but we know it needs an **&&** combinator
```scala
trait Prop {def &&(p: Prop): Prop }
```
+++
#### Prop
... and we know it needs **check**
```scala
trait Prop {
  def check: Unit
  def &&(p: Prop): Prop
}
```
- .. But because it returns **Unit**, the only option for **&&** is to run both **check** methods |
- which would suck |
+++
.. so let's try returning Boolean..
```scala
trait Prop {
  def check: Boolean
  def &&(p: Prop): Prop
}
```
### Exercise 8.3
#### Implement &&
+++



#### Remember Irek's [Purely functional state ?](https://docs.google.com/presentation/d/1Q1DfELS6b2xTfvRYDx0VQRhpTX8c2085ScbvUjsfn6I/edit#slide=id.g2316352f05_0_99)  



```python
from time import localtime

activities = {8: 'Sleeping', 9: 'Commuting', 17: 'Working',
              18: 'Commuting', 20: 'Eating', 22: 'Resting' }

time_now = localtime()
hour = time_now.tm_hour

for activity_time in sorted(activities.keys()):
    if hour < activity_time:
        print activities[activity_time]
        break
else:
    print 'Unknown, AFK or sleeping!'
```

###### Code-blocks let you present any <p> **static code** with auto-syntax highlighting

---

### Code-Blocks
##### Using
#### **Code-Presenting**

![Press Down Key](assets/down-arrow.png)

+++

```python
from time import localtime

activities = {8: 'Sleeping', 9: 'Commuting', 17: 'Working',
              18: 'Commuting', 20: 'Eating', 22: 'Resting' }

time_now = localtime()
hour = time_now.tm_hour

for activity_time in sorted(activities.keys()):
    if hour < activity_time:
        print activities[activity_time]
        break
else:
    print 'Unknown, AFK or sleeping!'
```

@[1]
@[3-4]
@[6-7]
@[9-14]

###### Use code-presenting to **step-thru** code <p> from directly within your presentation


---

### Code-Blocks
##### Using
#### Code-Presenting
#### **With Annotations**

![Press Down Key](assets/down-arrow.png)

+++

```python
from time import localtime

activities = {8: 'Sleeping', 9: 'Commuting', 17: 'Working',
              18: 'Commuting', 20: 'Eating', 22: 'Resting' }

time_now = localtime()
hour = time_now.tm_hour

for activity_time in sorted(activities.keys()):
    if hour < activity_time:
        print activities[activity_time]
        break
else:
    print 'Unknown, AFK or sleeping!'
```

@[1](Python from..import statement)
@[3-4](Python dictionary initialization block)
@[6-7](Python working with time)
@[9-14](Python for..else statement)

---

### Naturally
### Code-Presenting
### works in exactly the same way on [Code-Delimiter Slides](https://github.com/gitpitch/gitpitch/wiki/Code-Delimiter-Slides) as it does on [Code-Blocks](https://github.com/gitpitch/gitpitch/wiki/Code-Slides).

---

### Code-Delimiter Slides

```
                  ---?code=path/to/source.file
```

#### The Basics

![Press Down Key](assets/down-arrow.png)

+++?code=src/python/time.py&lang=python

###### Code delimiters let you present any <p> **code file** with auto-syntax highlighting

---

### Code-Delimiter Slides
##### Using
#### **Code-Presenting**

![Press Down Key](assets/down-arrow.png)

+++?code=src/javascript/config.js&lang=javascript

@[1-3]
@[5-8]
@[10-16]
@[18-24]

###### Use code-presenting to **step-thru** code <p> from directly within your presentation

---

### Code-Delimiter Slides
##### Using
#### Code-Presenting
#### **With Annotations**

![Press Down Key](assets/down-arrow.png)

+++?code=src/elixir/monitor.ex&lang=elixir

@[11-14](Elixir module-attributes as constants)
@[22-28](Elixir with-statement for conciseness)
@[171-177](Elixir case-statement pattern matching)
@[179-185](Elixir pipe-mechanism for composing functions)

---

### Learn By Example
#### View The [Presentation Markdown](https://github.com/gitpitch/code-presenting/blob/master/PITCHME.md)
