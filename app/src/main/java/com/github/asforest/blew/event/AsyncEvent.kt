package com.github.asforest.blew.event


class AsyncEvent : Iterable<AsyncEvent.Listener>
{
    val listeners = mutableListOf<Listener>()

    fun always(cb: suspend () -> Unit)
    {
        addListener(cb, ListenerType.ALWAYS)
    }

    fun once(cb: suspend () -> Unit)
    {
        addListener(cb, ListenerType.ONCE)
    }

    private fun addListener(callback: suspend () -> Unit, type: ListenerType)
    {
        this += Listener(callback, type)
    }

    operator fun plusAssign(listener: Listener)
    {
        if(listener in this)
            throw RuntimeException("The listener has already been added")

        listeners += listener
    }

    operator fun minusAssign(listener: Listener)
    {
        if(listener !in this)
            throw RuntimeException("The Listener not found")

        listeners -= listener
    }

    suspend fun invoke()
    {
        val validListeners = listeners.filter { it.type != ListenerType.NEVER }

        for (listener in validListeners)
        {
            listener.callback()

            if(listener.type == ListenerType.ONCE)
                listener.type = ListenerType.NEVER
        }

        listeners.removeIf { it.type == ListenerType.NEVER }
    }

    operator fun contains(listener: Listener): Boolean
    {
        return listener in listeners
    }

    override fun iterator(): MutableIterator<Listener>
    {
        return listeners.iterator()
    }

    class Listener(
        var callback: suspend () -> Unit,
        var type: ListenerType,
    )

    enum class ListenerType
    {
        NEVER, ONCE, ALWAYS
    }
}

