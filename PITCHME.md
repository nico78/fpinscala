## Functional Programming in Scala
### Chapter 8 - Property-Based Testing

---

### Property-Based Testing

#### What is it?

![Press Down Key](assets/down-arrow.png)

+++
- Technique for testing **laws** or **invariants** about the behaviour of your code |
- Decoupling specification of program behaviour from creation of test cases |
---
- The programmer focuses on specifying domain, behaviour and high-level constraints |
- The framework generates test cases |

Note:
The framework then automatically generates test cases that satisfy these constraints, and
runs tests to ensure that programs behave as specified

---
#### A brief tour of property-based testing

![Press Down Key](assets/down-arrow.png)
+++
```scala
val intList = Gen.listOf(Gen.choose(0,100))
val prop =
 forAll(intList)(ns => ns.reverse.reverse == ns) &&
 forAll(intList)(ns => ns.headOption == ns.reverse.lastOption)
```
@[1](A generator of lists of integers between 0 and 100.)
@[2](A property that specifies the behavior of the List.reverse method.)
@[3](Check that reversing a list twice gives back the original list)
@[4](Check that the first element becomes the last element after reversal.)
---

```scala
val failingProp = forAll(intList)(ns => ns.reverse == ns)
```
A property that is obviously false
---
![Logo](assets/generatorsProperties.png)
---
---?image=assets/generatorsProperties.png
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
