package kollect

import arrow.Kind
import arrow.core.Tuple2
import arrow.core.NonEmptyList
import arrow.core.extensions.nonemptylist.foldable.traverse_
import arrow.fx.typeclasses.Concurrent
import kotlinx.coroutines.Dispatchers

internal object KollectExecution {

    fun <F, A> parallel(CF: Concurrent<F>, effects: NonEmptyList<Kind<F, A>>): Kind<F, NonEmptyList<A>> = CF.run {
        effects.traverse(CF) { a -> a.startF(Dispatchers.IO) }.flatMap { fibers ->
            fibers.traverse(CF) { it.join() }.handleErrorWith { e ->
                fibers.traverse_(CF) { fiber -> fiber.cancel() } //If failed than cancel all running tasks
                        .map2(CF.raiseError(e), Tuple2<Unit, NonEmptyList<A>>::b)
            }
        }
    }
}
