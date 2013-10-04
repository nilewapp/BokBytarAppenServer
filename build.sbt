organization  := "com.mooo.nilewapps"

name := "BokbytarappenServer"

version       := "0.1"

scalaVersion  := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature")

resolvers ++= Seq(
  "spray repo" at "http://nightlies.spray.io/"
)

libraryDependencies ++= Seq(
  // spray
  "io.spray"            %   "spray-can"     % "1.1-20131004",
  "io.spray"            %   "spray-routing" % "1.1-20131004",
  "io.spray"            %   "spray-testkit" % "1.1-20131004",
  "io.spray"            %%  "spray-json"    % "1.2.5",
  "com.typesafe.akka"   %%  "akka-actor"    % "2.1.4",
  "org.specs2"          %%  "specs2"        % "1.13"    %   "test",
  // slick
  "com.typesafe.slick"  %%  "slick"         % "1.0.0",
  "org.slf4j"           %   "slf4j-nop"     % "1.6.4",
  "com.h2database"      %   "h2"            % "1.3.170",
  // config
  "com.typesafe"        %   "config"        % "1.0.2",
  // email
  "com.sun.mail"        %   "javax.mail"    % "1.5.0"
)

seq(Revolver.settings: _*)
