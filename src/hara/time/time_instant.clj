(ns hara.time.time_instant
  (:require [hara.protocol.time :as time]
            [hara.time.common :as common])
  (:import [java.time Instant]
           [java.time.temporal ChronoField]))

(extend-protocol time/IInstant
  Instant
  (-to-long      [t]
    (.toEpochMilli t))
  (-milli        [t]
    (.getLong t ChronoField/MILLI_OF_SECOND))
  (-second       [t]
    (.getLong t ChronoField/SECOND_OF_MINUTE))
  (-minute       [t]
    (.getLong t ChronoField/MINUTE_OF_HOUR))
  (-hour         [t]
    (.getLong t ChronoField/HOUR_OF_DAY))
  (-day          [t]
    (.getLong t ChronoField/DAY_OF_MONTH))
  (-day-of-week  [t]
    (.getLong t ChronoField/DAY_OF_WEEK))
  (-month        [t]
    (.getLong t ChronoField/MONTH_OF_YEAR))
  (-year         [t]
    (.getLong t ChronoField/YEAR))
  (-timezone     [t]
    ))
    
(defmethod common/from-long Instant
  [type long]
  (Instant/ofEpochMilli long))

