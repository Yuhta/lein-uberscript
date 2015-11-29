(ns leiningen.uberscript
  "Embed uberjar into an executable script."
  (:require [clojure.java.io :as io]
            [leiningen.core.main :as main]
            [leiningen.uberjar :as uberjar])
  (:import java.io.PrintStream
           (java.nio.file Files
                          LinkOption
                          attribute.PosixFilePermission)))

(defn- script-filename [project]
  (str (io/file (:target-path project)
                (or (:uberscript-name project) (str (:name project))))))

(defn- create-script [script jar]
  (io/make-parents script)
  (let [script-file (io/file script)
        script-path (.toPath script-file)]
    (with-open [out (io/output-stream script-file)]
      (doto (PrintStream. out)
        (.println "#!/usr/bin/env sh")
        (.println "exec java $JAVA_OPTS -jar \"$0\" \"$@\""))
      (io/copy (io/file jar) out))
    (main/info "Created" script)
    (let [perms (Files/getPosixFilePermissions script-path
                                               (make-array LinkOption 0))]
      (doseq [p (list PosixFilePermission/OWNER_EXECUTE
                      PosixFilePermission/GROUP_EXECUTE
                      PosixFilePermission/OTHERS_EXECUTE)]
        (.add perms p))
      (Files/setPosixFilePermissions script-path perms))))

(defn uberscript
  ([project main]
   (create-script (script-filename project)
                  (uberjar/uberjar project main)))
  ([project]
   (uberscript project nil)))
