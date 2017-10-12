package fpinscala.testing

import fpinscala.laziness.Stream
import fpinscala.state._
import fpinscala.parallelism._
import fpinscala.parallelism.Par.Par
import Gen._
import Prop._
import java.util.concurrent.{Executors,ExecutorService}

/*
The library developed in this chapter goes through several iterations. This file is just the
shell, which you can fill in and modify while working through the chapter.
*/
//
//trait Prop { self =>
//
//  def check: Boolean
//  def &&(p:Prop):Prop = {
//    new Prop {
//      def check = self.check && p.check
//    }
//  }
//}

object Result {
  type TestCases = Int
  type FailedCase = String
  type SuccessCount = Int

}
import Result._
sealed trait Result {
  def isFalsified: Boolean
}
case object Passed extends Result {
  def isFalsified = false
}
case class Falsified(failure: FailedCase,
                     successes: SuccessCount) extends Result {
  def isFalsified = true
}




case class Prop(run: (TestCases,RNG) => Result){

  def tag(msg: String) = Prop {
    (n, rng) => run(n, rng) match {
      case Falsified(fc, sc) => Falsified(msg + "\n" + fc, sc)
      case passed => passed
    }
  }
  def &&(p:Prop): Prop = {
    Prop{
      (n, rng) =>
        run(n, rng) match {
          case Passed => p.run(n, rng)
          case f => f
        }
    }
  }
  def ||(p:Prop): Prop = {
    Prop{
      (n, rng) =>
        run(n, rng) match {
          case Falsified(fc, sc) => p.tag(fc).run(n, rng)
          case passed => passed
        }
    }
  }
}






















object Prop {

  def choose(start: Int, stopExclusive: Int): Gen[Int] =
    Gen(State(rng => RNG.nonNegativeInt(rng) match {
      case (n, rng2) => (start + n % (stopExclusive-start), rng2)
    }))

  def choose2(start: Int, stopExclusive: Int): Gen[Int] =
    Gen(State(RNG.nonNegativeInt).map(n => start + n % (stopExclusive - start))))

  def forAll[A](gen: Gen[A])(f: A => Boolean): Prop = ???


  val sortedProp = forAll(Gen.list)

}

object Gen {
  // always generates value a
  def unit[A](a: => A): Gen[A] = Gen(State.unit(a))

  def boolean : Gen[Boolean] = Gen(State(RNG.boolean))

  def double : Gen[Double] = Gen(State(RNG.double))

  def union[A](g1: Gen[A], g2: Gen[A]): Gen[A]= {
    for {
      b <- boolean
      v <- if(b) g1 else g2
    } yield v
  }

  def weighted[A](g1: (Gen[A], Double), g2: (Gen[A], Double)): Gen[A] = {
    val g1Threshold = g1._2.abs / (g1._2.abs + g2._2.abs)
    for {
      d <- double
      v <- if (d < g1Threshold) g1 else g2
    } yield v
  }



}

case class Gen[A](sample: State[RNG, A]) {
  def map[B](f: A => B): Gen[B] = Gen(sample.map(f))

  def flatMap[B](f: A => Gen[B]): Gen[B] = Gen(sample.flatMap(a => f(a).sample))

  def listOfN(n: Int): Gen[List[A]] =
    Gen(State.sequence(List.fill(n)(sample)))

  def listOfN(size: Gen[Int]):Gen[List[A]] =
  for {
    n <- size
    l <- listOfN(n)
  } yield l

  def unsized: SGen[A] = SGen(_ => this)

  def map2[B,C](g: Gen[B])(f: (A,B) => C): Gen[C] =
    Gen(sample.map2(g.sample)(f))

  def **[B](g: Gen[B]): Gen[(A,B)] =
    (this map2 g)((_,_))
}


case class SGen[A](forSize: Int => Gen[A]) {


  def apply(n: Int): Gen[A] = forSize(n)
  def map[B](f: A => B): SGen[B] = SGen(forSize andThen (_ map f))

  def flatMap[B](f: A => SGen[B]): SGen[B] = SGen{ n =>
    forSize(n).flatMap(f(_).forSize(n) )
  }

  def **[B](s2: SGen[B]): SGen[(A,B)] =
    SGen(n => forSize(n) ** s2(n))
}

object SGen {

  def listOf[A](g: Gen[A]): SGen[List[A]] =
    SGen(n => g.listOfN(n))

  def nonEmptyListOf[A](g: Gen[A]): SGen[List[A]] =
    SGen(n => g.listOfN(n max 1))

  val l = listOf[Double](Gen.double)

  private val doubleListsLength32: Gen[List[Double]] = l.forSize(32)

}

















