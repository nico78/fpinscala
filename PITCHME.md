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

def forAllPar[A]​(g: Gen[A])(f: A => Par[Boolean]): Prop =
  forAll(S ** g) { case (s,a) => f(a)(s).get }
```

+++
We can even use `**` as a custom extractor for `case` pattern match:

```scala
object ** {
  def unapply[A,B]​(p: (A,B)) = Some(p)
}

def forAllPar[A]​(g: Gen[A])(f: A => Par[Boolean]): Prop =
  forAll(S ** g) { case s ** a => f(a)(s).get }
```

+++

Now if we define:

```scala
def checkPar(p: Par[Boolean]​): Prop =
  forAllPar(Gen.unit(()))(_ => p)
```
Our property looks a lot cleaner:

```scala
val p2 = checkPar {
  equal (
    Par.map(Par.unit(1))(_ + 1),
    Par.unit(2)
  )
}
```

+++
### Other properties from Chapter 7

We generalised:

```scala
map(unit(x))(f) == unit(f(x))
```

to:
```scala
map(y)(x => x) == y
```

@[2](We can't express this as it states equality holds for all choices of y, for all types)

+++
we're forced to pick values for `y`:

```scala
val pint = Gen.choose(0,10) map (Par.unit(_))
val p4 =
  forAllPar(pint)(n => equal(Par.map(n)(y => y), n))
```
Note:
We can certainly range over more choices of y , but what we have here is probably good
enough. The implementation of map can’t care about the values of our parallel computation, so there isn’t much point in constructing the same test for Double , String ,
and so on. What can affect map is the structure of the parallel computation. If we
wanted greater assurance that our property held, we could provide richer generators
for the structure. Here, we’re only supplying Par expressions with one level of nesting.

+++
### Exercise 8.16
Write a richer generator for `Par[Int]` which builds more deeply nested parallel computations
+++
### Exercise 8.17
Express the propery about `fork`:

```scala
  fork(x) == x
```

---
### Testing higher-order functions and future directions
![Press Down Key](assets/down-arrow.png)
+++
- We don't currently have a way to test higher-order functions.|
- We can generate data with generators but not functions |
+++
e.g. we want to express that `takeWhile(f)` performs correctly for any `f` |
```scala
//pseudo-code:
forall(functions ** lists){
  case (f, l) => l.takeWhile(f).forall(f) == true
}
```
@[2](any value in the list left after takewhile satisfies the predicate)

+++
### Exercise 8.16
Come up with some other properties that takeWhile should satisfy.
Can you think of a good property expressing the relationship between takeWhile and dropWhile ?

+++
We could start with a more specific property:

```scala
val isEven = (i: Int) => i%2 == 0
val takeWhileProp =
  Prop.forAll(Gen.listOf(int))(ns => ns.takeWhile(isEven).forall(isEven))
```
But how to generalise over other functions?

+++
- Suppose we have a `Gen[Int]` and would like to produce a `Gen[String => Int]` |
- we could produce String => Int functions that simply ignore their input string and delegate to the underlying Gen[Int] |
- but that's not quite satisfactory - these are just constant functions

+++

Let's start by looking at the signature of our motivating example, generating a function from `String => Int` given a `Gen[Int]`:

```scala
  def genStringInt(g: Gen[Int]): Gen[String => Int]
```
+++

And let's generalize this a bit to not be specialized to `Int`, because that would let us cheat a bit (by, say, returning the `hashCode` of the input `String`, which just so happens to be an `Int`).

```scala
def genStringFn[A](g: Gen[A]): Gen[String => A]
```
Note:
We've already ruled out just returning a function that ignores the input `String`, since that's not very interesting!
Instead, we want to make sure we _use information from_ the input `String` to influence what `A` we generate. How can we do that? Well, the only way we can have any influence on what value a `Gen` produces is to modify the `RNG` value it receives as input:

+++

Recall our definition of `Gen`:

```scala
case class Gen[+A](sample: State[RNG,A]​)
```

Just by following the types, we can start writing:

```scala
def genStringFn[A](g: Gen[A]​): Gen[String => A] = Gen {
  State { (rng: RNG) => ??? }
}
```

Where `???` has to be of type `(String => A, RNG)`, and moreover, we want the `String` to somehow affect what `A` is generated. We do that by modifying the seed of the `RNG` before passing it to the `Gen[A]` sample function. A simple way of doing this is to compute the hash of the input string, and mix this into the `RNG` state before using it to produce an `A`:

```scala
def genStringFn[A](g: Gen[A]​): Gen[String => A] = Gen {
  State { (rng: RNG) =>
    val (seed, rng2) = rng.nextInt // we still use `rng` to produce a seed, so we get a new function each time
    val f = (s: String) => g.sample.run(RNG.Simple(seed.toLong ^ s.hashCode.toLong))._1
    (f, rng2)
  }
}
```

More generally, any function which takes a `String` and an `RNG` and produces a new `RNG` could be used. Here, we're computing the `hashCode` of the `String` and then XOR'ing it with a seed value to produce a new `RNG`. We could just as easily take the length of the `String` and use this value to perturn our RNG state, or take the first 3 characters of the string. The choices affect what sort of function we are producing:

* If we use `hashCode` to perturb the `RNG` state, the function we are generating uses all the information of the `String` to influence the `A` value generated. Only input strings that share the same `hashCode` are guaranteed to produce the same `A`.
* If we use the `length`, the function we are generating is using only some of the information of the `String` to influence the `A` being generated. For all input strings that have the same length, we are guaranteed to get the same `A`.

The strategy we pick depends on what functions we think are realistic for our tests. Do we want functions that use all available information to produce a result, or are we more interested in functions that use only bits and pieces of their input? We can wrap the policy up in a `trait`:

```scala
trait Cogen[-A]​ {
  def sample(a: A, rng: RNG): RNG
}
```

As an exercise, try implementing a generalized version of `genStringFn`.

```scala
def fn[A,B](in: Cogen[A]​)(out: Gen[B]): Gen[A => B]
```

You can pattern the implementation after `genStringFn`. Just follow the types!

One problem with this approach is reporting test case failures back to the user. In the event of a failure, all the user will see is that for some opaque function, the property failed, which isn't very enlightening. There's been work in the Haskell library [QuickCheck](http://www.cse.chalmers.se/~rjmh/QuickCheck/manual.html) to be able to report back to the user and even _shrink_ down the generated functions to the simplest form that still falsifies the property. See [this talk on shrinking and showing functions](https://www.youtube.com/watch?v=CH8UQJiv9Q4).



```scala
]​):
```
