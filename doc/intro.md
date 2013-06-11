## Introduction to pachinko

# pachinko is a pseudo-frp library for clojure

frp is functional reactive programming, a model for propagating state in a
functional program

I say pseudo-frp because pachinko was my attempt to figure out how frp works, by
implementing a version of it. I have no doubt that this library does not
implement true canonical functional reactive programming. But what I ended up
with is useful (if a bit odd).

The weirdest thing about pachinko is probably the >> macro, which was an
experiment designed to get more readable code (it worked for me, but our
opinions of readable will vary).



## A short summary of >>

### the following two forms are equivalent:

`(fn [message params {:keys [x y]}]
   (let [z (+ x y)]
     (println z)
     [[:message [message params z]]
      [:other-message [params message x y z]]
      [:message [params message z]]]))`
   
`(do (require '[pachinko.dataflow :as >])
     (>/>> [message params]
           -| x y |-
           -= z (+ x y) =-
           -_ (println z) _-
           ...
           :message [message params z]
           :other-message [params message x y z]
           :message [params message z]))`

### reasons to use >> -

it simplifies conventions that are common in pachinko usage
for some it has less visual noise and quick indications of what is being done

### reasons not to use >> -

it doesn't look like normal clojure code
the weird delimiter symbols are ugly
editors don't support this custom "syntax"