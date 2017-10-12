## Functional Programming in Scala
### Chapter 8 - Property-Based Testing

---

### Property-Based Testing - RECAP

![Press Down Key](assets/down-arrow.png)

+++
- Technique for testing **laws** or _invariants_ about the behaviour of your code |
- Decoupling specification of program behaviour from creation of test cases |
+++
- The programmer focuses on specifying domain, behaviour and high-level constraints |
- The framework generates test cases |

Note:
The framework then automatically generates test cases that satisfy these constraints, and
runs tests to ensure that programs behave as specified

+++
```scala
val intList = Gen.listOf(Gen.choose(0,100))
val prop =
 forAll(intList)(ns => ns.reverse.reverse == ns) &&
 forAll(intList)(ns => ns.headOption == ns.reverse.lastOption)
```
@[1](A **generator** of lists of integers between 0 and 100.)
@[2](A **property** that specifies the **behavior** of the List.reverse method.)
@[3](Check that reversing a list twice gives back the original list)
@[4](Check that the first element becomes the last element after reversal.)
+++

### current definitions
forall
```scala
def forAll[A]​(a: Gen[A])(f: A => Boolean): Prop
```
Prop
```scala
trait Prop {
  def run: Either[(FailedCase, SuccessCount), SuccessCount]
}```
Gen
```scala
case class Gen[A] (sample: State[RNG, A])
```
+++
**Prop** is currently a lazy Either

```scala
trait Prop {
  def check: Either[(FailedCase, SuccessCount), SuccessCount]
}```
What's missing?
- We don't know how to specify what constitutes "success" - "how many test cases need to pass"? |
- rather than hardcode, we'll abstract over the dependency:

---
### Our API - Continuing

![Press Down Key](assets/down-arrow.png)
+++

Abstracting over number of required test cases:
```scala
type TestCases = Int
type Result = Either[(FailedCase, SuccessCount), SuccessCount]​
case class Prop(run: TestCases => Result)
```
@[2](We don't really need SuccessCount on RHS of Either any more)
+++
Abstracting over number of required test cases:
```scala
type TestCases = Int
type Result = Option[(FailedCase, SuccessCount)]​
case class Prop(run: TestCases => Result)
```
@[2](But now `None` will mean it passed... bit weird.. create a new type)
+++
```scala
sealed trait Result {
  def isFalsified: Boolean
}
case object Passed extends Result {
  def isFalsified = false
}

case class Falsified(
  failure: FailedCase,
  successes: SuccessCount) extends Result {
  def isFalsified = true
}
```
@[4](Indicates that all tests passed)
@[8](Indicates that one of the test cases falsified the property)

+++
### forall again
```scala
def forAll[A]​(a: Gen[A])(f: A => Boolean): Prop

```
forAll doesn’t have enough information to return a Prop
- needs number of test cases to try |
- needs an RNG |

+++
So we'll add dependency to `Prop` :
```scala
case class Prop(run: TestCases => Result)
```
becomes:
```scala
case class Prop(run: (TestCases,RNG) => Result)
```
Note:
If we think of other dependencies that it might need, besides the number of test
cases and the source of randomness, we can just add these as extra parameters to
Prop.run later.

+++
### Implementing forall
turning `Gen` into a random `Stream`:
```scala
def randomStream[A]​(g: Gen[A]​)(rng: RNG): Stream[A] =
   Stream.unfold(rng)(rng => Some(g.sample.run(rng)))
```
@[2](Generates an infinite stream of A values by repeatedly sampling a generator.)

+++
```scala
def forAll[A]​(as: Gen[A])(f: A => Boolean): Prop = Prop {
  (n,rng) => randomStream(as)(rng).zip(Stream.from(0)).take(n).map {
    case (a, i) => try {
      if (f(a)) Passed else Falsified(a.toString, i)
    } catch { case e: Exception => Falsified(buildMsg(a, e), i) }
  }.find(_.isFalsified).getOrElse(Passed)
```
@[2](A stream of pairs `a,i` where `a` is a random value and i is its index in the stream.)
@[4](When a test fails, record failed case & index: how many succeeded before failure)
@[5](If a test case generates an exception, record it in the result.)
+++
```scala
def buildMsg[A]​(s: A, e: Exception): String =
   s"generated an exception: ${e.getMessage}\n" +
   s"test case: $s\n" +
   s"stack trace:\n ${e.getStackTrace.mkString("\n")}"
```
+++
### Exercise 8.9
#### implement && and || for the new Prop
```scala
  def &&(p: Prop): Prop
  def ||(p: Prop): Prop
```
Can we find a way to assign a label so we know which property failed?

---

### Test case minimization

![Press Down Key](assets/down-arrow.png)
+++
Find the smallest or simplest failing test case to illustrate the problem.
Two approaches:
- Shrinking - once a failure found, reduce size till stops failing|
- Sized generation - start small|
- we'll use sized generation - simpler - but most libs use shrinking |
Note:
shrinking:
requires us to write separate
code for each data type to implement this minimization process.
 Sized:
Rather than shrinking test cases after the fact, we simply gener-
ate our test cases in order of increasing size and complexity. So we start small
and increase the size until we find a failure. This idea can be extended in vari-
ous ways to allow the test runner to make larger jumps in the space of possible
sizes while still making it possible to find the smallest failing test.

will use sized since avoids having to implement search in space of test cases
+++
A new type for sized generation:
```scala
  case class SGen[+A]​(forSize: Int => Gen[A])
```
@[1](Takes a size, produces a generator for that size)
+++
### Exercise 8.10/11
#### Implement helper functions for converting Gen to SGen
```scala
  def unsized: SGen[A]​
```
and include convenience functions on SGen that simply delegate to the corresponding
 functions on Gen
+++
### Exercise 8.12
#### Implement listOf combinator that doesn’t accept an explicit size.
```scala
  def listOf[A]​(g: Gen[A]): SGen[List[A]​]
```
The implementation should generate lists of the requested size.

---
### The sized version of forall
```scala
/*was*/ def forAll[A]​(as: Gen[A])(f: A => Boolean): Prop
/*now*/ def forAll[A]​(g: SGen[A])(f: A => Boolean): Prop
```
@[2](Impossible to implement:  SGen is expecting a size, but Prop doesn’t receive any size information.)
Note:
we want to put Prop in charge of invoking the underlying generators with various sizes,so we’ll have Prop accept a maximum size
+++
### Enhancing prop with MaxSize

```scala
case class Prop(run: (TestCases,RNG) => Result)
```
becomes:
```scala
type MaxSize = Int
case class Prop(run: (MaxSize,TestCases,RNG) => Result)
```
+++

```scala
def forAll[A]​(g: Int => Gen[A])(f: A => Boolean): Prop = Prop {
  (max,n,rng) =>
   val casesPerSize = (n + (max - 1)) / max
   val props: Stream[Prop] =
     Stream.from(0).take((n min max) + 1).map(i => forAll(g(i))(f))
   val prop: Prop =
     props.map(p => Prop { (max, _, rng) =>
       p.run(max, casesPerSize, rng)
     }).toList.reduce(_ && _)
   prop.run(max,n,rng)
```]​(
### Trying with max
```scala
val smallInt = Gen.choose(-10,10)
val maxProp = forAll(listOf(smallInt)) { ns =>
  val max = ns.max
  !ns.exists(_ > max)
}
maxProp.run(maxSize = 100, testCases = 100,
  rng = RNG.Simple(System.currentTimeMillis))
```
@[4](No value greater than max should exist in ns)
@[6](A bit cumbersome to run...)

---
### a run helper

```scala
def run(p: Prop,
        maxSize: Int = 100,
        testCases: Int = 100,
        rng: RNG = RNG.Simple(System.currentTimeMillis)): Unit =
  p.run(maxSize, testCases, rng) match {
    case Falsified(msg, n) =>
      println(s"! Falsified after $n passed tests:\n $msg")
    case Passed =>
      println(s"+ OK, passed $testCases tests.")
  }
```
+++
### running it

```scala
  run(maxProp)
```
Let's run this code...

+++
### Exercise 8.13
Define `listOf1` for generating nonempty lists, and then update your specification of
max to use this generator.
+++
### Exercise 8.14
Write a property to verify the behavior of `List.sorted`
For instance, `List(2,1,3).sorted` is equal to `List(1,2,3)``
---
### Writing a test suite for parallel computations
![Press Down Key](assets/down-arrow.png)
+++
### Recall computation laws
how would we express this?
```scala
map(unit(1))(_ + 1) == unit(2)
```
+++
doable but ugly:
```scala
  val ES: ExecutorService = Executors.newCachedThreadPool
  val p1 = Prop.forAll(Gen.unit(Par.unit(1)))(i =>
  Par.map(i)(_ + 1)(ES).get == Par.unit(2)(ES).get)
```
Note:
we've muddied the idea of the test with other details

Also, we aren't varying the input we just have a hardcoded example.

+++
### Proving properties

To improve, note `forAll` is too general

Introduce a combinator for hardcoded examples:
```scala
def check(p: => Boolean): Prop = {
  lazy val result = p
  forAll(unit(()))(_ => result)
}
```
@[1](Non-strict here)
@[2](memoized to avoid recomputation.. but test runner will still run multiple times)

+++
e.g.
```scala
run(check(true))
```
@[1](will test property 100 times)

- We need a new primitive... |
+++
Prop is currently:

```scala
case class Prop(run: (MaxSize,TestCases,RNG) => Result)
```

so `check` could be:

```scala
def check(p: => Boolean): Prop = Prop { (_, _, _) =>
  if (p) Passed else Falsified("()", 0)
}
```
@[1](Result: Passed | Falsified)
@[3](We need a new kind of Result)

Note:
This is certainly better than using forAll , but run(check(true)) will still print “passed
100 tests” even though it only tests the property once. It’s not really true that such a
property has “passed” in the sense that it remains unfalsified after a number of tests. It
is proved after just one test.

+++
```scala
case object Proved extends Result
```

```scala
def check(p: => Boolean): Prop = Prop { (_, _, _) =>
  if (p) Proved else Falsified("()", 0)
}
```
+++

```scala
def run(p: Prop,
        maxSize: Int = 100,
        testCases: Int = 100,
        rng: RNG = RNG.Simple(System.currentTimeMillis)): Unit =
  p.run(maxSize, testCases, rng) match {
    case Falsified(msg, n) =>
      println(s"! Falsified after $n passed tests:\n $msg")
    case Passed =>
      println(s"+ OK, passed $testCases tests.")
      //new:
    case Proved =>
      println(s"+ OK, proved property.")
  }
```
+++
### Exercise 8.15

Some `forall` properties can be proven as well as `check` ones.
Take a look at https://github.com/fpinscala/fpinscala/blob/master/answers/src/main/scala/fpinscala/testing/Exhaustive.scala
for automatically proving our code correct.

---
### Back to Testing Par
Proving that
```scala
Par.map(Par.unit(1))(_ + 1) == Par.unit(2)
```

```scala
  val pp = Prop.check {
    val p = Par.map(Par.unit(1))(_ + 1)
    val p2 = Par.unit(2)
    p(ES).get == p2(ES).get
  }
```
Note:
still a bit ugly to be using `.get` plus it needs to know implementation details of Par to compare for equality

+++
so `lift` equality comparison to `Par`:

```scala
def equal[A]​(p: Par[A], p2: Par[A]): Par[Boolean] =
  Par.map2(p,p2)(_ == _)

val p3 = check {
  equal(
   Par.map(Par.unit(1))(_ + 1),
   Par.unit(2)
  )(ES).get
}
```
+++
... and we can just move running of Par out:
```scala
def forAllPar[A]​(g: Gen[A])(f: A => Par[Boolean])
```
... and add some variation across parallel strategies:
```scala
val S = weighted(
  choose(1,4).map(Executors.newFixedThreadPool) -> .75,
  unit(Executors.newCachedThreadPool) -> .25
)

def forAllPar[A]​(g: Gen[A])(f: A => Par[Boolean]): Prop =
  forAll(S.map2(g)((_,_))) { case (s,a) => f(a)(s).get }
```
@[2](This generator creates a fixed thread pool executor 75% of the time and an unbounded one 25% of the time.)

+++
But is this a bit too noisy just to combine generators into a pair?
```scala
S.map2(g)((_,_))
```
How about:

```scala
def **[B]​(g: Gen[B]): Gen[(A,B)] =
  (this map2 g)((_,_))

def forAllPar[A](g: Gen[A])(f: A => Par[Boolean]): Prop =
  forAll(S ** g) { case (s,a) => f(a)(s).get }
```
