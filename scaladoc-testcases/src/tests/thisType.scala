package tests
package thisType


class X[Map[_, _[_]]]:
  // issue 16024
  inline def map[F[_]](f: [t] => t => F[t]): Map[this.type, F]
    = ???


sealed trait Tuple[Y[_]]
{
  def ++[This >: this.type <: Tuple[Y]](that: Y[Tuple[Y]]): Any
    = ???
}

sealed trait NonEmptyTuple extends Tuple[Option]

trait Foo[X]
{
  def foo0[T <: Foo.this.type](x: X): Foo.this.type //expected: def foo0[T <: this.type](x: X): this.type
    = bar0[T](x)
  def bar0[T <: this.type](x: X): this.type
    = foo0[T](x)

  sealed abstract class Nested[+H, +T <: (Tuple), A <: Tuple[List]] extends NonEmptyTuple with Foo[Int]
  //                                     ^^^^^^^ TODO fix
  {
    //expected: def ++[This >: this.type <: Tuple[Option]](that: Option[Tuple[Option]]): Any
    // TODO fix def ++[This >: Tuple.this.type <: Tuple[Option]](that: Option[Tuple[Option]]): Any

    //expected: def foo0[T <: this.type](x: Int): this.type
    // TODO fix def foo0[T <: this.type](x: X): this.type

    //expected: def bar0[T <: this.type](x: Int): this.type
    // TODO fix def bar0[T <: this.type](x: X): this.type

    def foo1[T <: Foo.this.type]: Nothing
    // TODO fix: def foo1[T <: this.type]: Nothing
      = ???

    def foo2[T <: this.type]: Nothing
      = ???
  }
}
