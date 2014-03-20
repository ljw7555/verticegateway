/* 
** Copyright [2012-2013] [Megam Systems]
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
** http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/
package models

import scalaz._
import scalaz.syntax.SemigroupOps
import scalaz.NonEmptyList._
import scalaz.Validation._
import scalaz.effect.IO
import scalaz.EitherT._
import com.twitter.util.Time
import Scalaz._
import controllers.stack._
import controllers.Constants._
import controllers.funnel.FunnelErrors._
import models._
import models.cache._
import com.stackmob.scaliak._
import com.basho.riak.client.query.indexes.{ RiakIndexes, IntIndex, BinIndex }
import com.basho.riak.client.http.util.{ Constants => RiakConstants }
import org.megam.common.riak.{ GSRiak, GunnySack }
import org.megam.common.uid.UID
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import java.nio.charset.Charset

/**
 * @author rajthilak
 *
 */
/* name, logo, catagory, pricetype, attach, approved, feature-1,2,3,4, plan->{price, description, type}, predefnode */

case class MarketPlacePlan(price: String, description: String, plantype: String) {
  val json = "\"price\":\"" + price + "\",\"description\":\"" + description + "\",\"plantype\":\"" + plantype + "\""
}

//lets make sure that we have atleast 2 features.
case class MarketPlaceFeatures(feature1: String, feature2: String, feature3: String = null, feature4: String = null) {
  val json = "\"feature1\":\"" + feature1 +  "\",\"feature2\":\"" + feature2 + "\",\"feature3\":\"" + feature3 +  "\",\"feature4\":\"" + feature4 + "\""
}

case class MarketPlaceAppDetails(logo: String, category: String, version: String, description: String) {
  val json = "\"logo\":\"" + logo + "\",\"category\":\"" + category + "\",\"version\":\"" + version + "\",\"description\":\"" + description + "\""
}

case class MarketPlaceAppLinks(free_support: String, paid_support: String, home_link: String, info_link: String, content_link: String, wiki_link: String, source_link: String) {
  val json = "\"free_support\":\"" + free_support + "\",\"paid_support\":\"" + paid_support + "\",\"home_link\":\"" + home_link + "\",\"info_link\":\"" + info_link + "\",\"content_link\":\"" + content_link + "\",\"wiki_link\":\"" + wiki_link + "\",\"source_link\":\"" + source_link + "\""
}

case class MarketPlaceInput(name: String, appdetails: MarketPlaceAppDetails, pricetype: String, features: MarketPlaceFeatures, plan: MarketPlacePlan, applinks: MarketPlaceAppLinks, attach: String, predefnode: String, approved: String) {
  val json = "{\"name\":\"" + name + "\",\"appdetails\":{" + appdetails.json + "},\"pricetype\":\"" + pricetype + "\",\"features\":{" + features.json + "},\"plan\":{" + plan.json + "},\"applinks\":{" + applinks.json + "},\"attach\":\"" + attach + "\",\"predefnode\":\"" + predefnode + "\",\"approved\":\"" + approved + "\"}"
}

//init the default market place addons
object MarketPlaceInput {

  val toMap = Map[String, MarketPlaceInput](
    "Alfresco" -> MarketPlaceInput("Alfresco", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/alfresco.png", "ECM", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 alfresco app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "Diaspora" -> MarketPlaceInput("Diaspora", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/diaspora.png", "Social", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 diaspora app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "DokuWiki" -> MarketPlaceInput("DokuWiki", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/dokuwiki.png", "Wiki", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 dokuwiki app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "DRBD" -> MarketPlaceInput("DRBD", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/drbd.png", "DR", "2:8.4.3-0ubuntu1", "The Distributed Replicated Block Device (DRBD) is a distributed replicated storage system for the Linux platform."), "free", new MarketPlaceFeatures("Backup your drive to any cloud, simply provide a directory (/var/lib/data).", "Enhances your app/server to be highly available, load balanced on multi cloud.", "Long-distance replication with DRBD Proxy (paid product).", "Launch new apps/services which are highly available."), new MarketPlacePlan("0", "Disaster recovery for 1 app or service", "free"), new MarketPlaceAppLinks("", "http://www.linbit.com/en/products-and-services/drbd-support/pricing/pricing-euro", "http://www.drbd.org/", "http://www.drbd.org/home/what-is-drbd/", "http://www.drbd.org/docs/about/", "http://www.drbd.org/users-guide-9.0/", "http://git.drbd.org/"), "true", "predefnode", "true"),
    "DreamFactory" -> MarketPlaceInput("DreamFactory", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/dreamfactory.png", "Mobile", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 dreamfactory app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "Drupal" -> MarketPlaceInput("Drupal", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/drupal.png", "CMS", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 drupal app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "Elgg" -> MarketPlaceInput("Elgg", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/elgg.png", "Social", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 elgg app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "Firepad" -> MarketPlaceInput("Firepad", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/firepad.png", "DevelopmentPlatform", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 firepad app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "Ghost" -> MarketPlaceInput("Ghost", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/ghost.png", "Blog", "0.4.1", "Ghost is an Open Source application which allows you to write and publish your own blog, giving you the tools to make it easy and even fun to do."), "free", new MarketPlaceFeatures("Ghost has an incredibly simple concept for writing. You write in Markdown on the left, and you see an immediate preview of exactly what your post will look like in rendered HTML on the right", "The Ghost Open Marketplace contains a large directory of themes, apps, and resources which have been created for Ghost.", "Drag and drop the widgets to create your own custom dashboard, with the most important information first.",""), new MarketPlacePlan("0", "create 1 ghost app", "free"), new MarketPlaceAppLinks("https://ghost.org/forum/", "#", "https://ghost.org/", "https://ghost.org/features/", "https://ghost.org/about/", "http://en.wikipedia.org/wiki/Ghost_(blogging_platform)", "https://github.com/tryghost/Ghost"), "false", "predefnode", "true"),
    "Gitlab" -> MarketPlaceInput("Gitlab", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/gitlab.png", "DevelopmentPlatform", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 gitlab app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "Hadoop" -> MarketPlaceInput("Hadoop", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/hadoop.png", "BusinessIntelligence", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 hadoop app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "Jenkins" -> MarketPlaceInput("Jenkins", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/jenkins.png", "DevelopmentPlatform", "1.554", "<h5><dfn>Jenkins CI is the leading open-source continuous integration server. Built with Java, it provides atleast 800+ plugins to support building and testing virtually any project."), "free", new MarketPlaceFeatures("Jenkins can be configured entirely from its friendly web GUI with extensive on-the-fly error checks and inline help. There's no need to tweak XML manually anymore, although if you'd like to do so, you can do that.", "Jenkins gives you clean readable URLs for most of its pages, including some permalinks like 'latest build' / 'latest successful build'.", "JUnit test reports can be tabulated, summarized, and displayed with history information.", "Jenkins can keep track of which build produced which jars, and which build is using which version of jars, and so on."), new MarketPlacePlan("0", "Use 1 continous integration engine", "free"), new MarketPlaceAppLinks("https://groups.google.com/forum/#!forum/jenkinsci-users", "#", "http://jenkins-ci.org/", "http://jenkins-ci.org/node", "http://jenkins-ci.org/node", "https://wiki.jenkins-ci.org/display/JENKINS/Home", "https://github.com/jenkinsci/jenkins"), "false", "predefnode", "true"),
    "Joomla" -> MarketPlaceInput("Joomla", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/joomla.png", "CMS", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 joomla app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "Liferay" -> MarketPlaceInput("Liferay", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/liferay.png", "Collaboration", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 liferay app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "Magneto" -> MarketPlaceInput("Magneto", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/magneto.png", "e-Commerce", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 magneto app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "MediaWiki" -> MarketPlaceInput("MediaWiki", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/mediawiki.png", "Wiki", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 mediawiki app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "OpenAM" -> MarketPlaceInput("OpenAM", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/openam.png", "Identity", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 openam app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "OpenAtrium" -> MarketPlaceInput("OpenAtrium", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/openatrium.png", "ProjectManagement", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 openatrium app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "OpenDJ" -> MarketPlaceInput("OpenDJ", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/opendj.png", "DirectoryServices", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 opendj app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "OpenERP" -> MarketPlaceInput("OpenERP", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/openerp.png", "ERP", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 openerp app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "OpenLDAP" -> MarketPlaceInput("OpenLDAP", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/openldap.png", "DirectoryServices", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 openldap app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "OTRS" -> MarketPlaceInput("OTRS", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/otrs.png", "HelpDesk", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 otrs app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "ownCloud" -> MarketPlaceInput("ownCloud", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/owncloud.png", "Collaboration", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 owncloud app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "Redmine" -> MarketPlaceInput("Redmine", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/redmine.png", "ProjectManagement", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 redmine app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "ReviewBoard" -> MarketPlaceInput("ReviewBoard", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/reviewboard.png", "DevelopmentPlatform", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 reviewboard app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "Riak" -> MarketPlaceInput("Riak", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/riak.png", "DB", "2.0.0", "Riak is a <span class='label label-info'>distributed database</span> designed to deliver maximum data availability by <span class='label label-info'>distributing data across multiple servers</span>."), "free", new MarketPlaceFeatures("Riak replicates and retrieves data intelligently so it is available for read and write operations, even in failure conditions. You can lose access to many nodes due to network partition or hardware failure without losing data. Add new machines to your Riak cluster easily without incurring a larger operational burden – the same ops tasks apply to small clusters as large clusters.", "Riak automatically distributes data around the cluster and yields a near-linear performance increase as you add capacity. Use Riak’s distributed, full-text search engine with a robust query language. Tag objects stored in Riak with additional values and query by exact match or range.", "Non-key-based querying for large datasets.Data is distributed across nodes using consistent hashing. Consistent hashing ensures data is evenly distributed around the cluster and new nodes can be added automatically, with minimal reshuffling. Riak uses Folsom, an Erlang-based system that collects and reports real-time metrics, to provide stats via HTTP request.", ""), new MarketPlacePlan("0", "Create 1 database", "free"), new MarketPlaceAppLinks("http://docs.basho.com/riak/latest/community/", "http://basho.com/contact/", "http://basho.com/riak/", "http://docs.basho.com/", "http://basho.com/", "http://en.wikipedia.org/wiki/Riak", "https://github.com/basho/riak"), "true", "predefnode", "true"),
    "SCM-Manager" -> MarketPlaceInput("SCM-Manager", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/scmmanager.png", "DevelopmentPlatform", "1.36", "The easiest way to share and manage your <span class='label label-info'>Git, Mercurial and Subversion</span> repositories over http"), "free", new MarketPlaceFeatures("SCM-Manager is completely configureable from its Web-Interface. No Apache and no database installation is required. Central user, group and permission management.", "Out of the box support for Git, Mercurial and Subversion. Full RESTFul Web Service API (JSON and XML). Rich User Interface.", "Trigger builds directly to cloud using scm-megam plugin", ""), new MarketPlacePlan("0", "Create 1 scm-manager app", "free"), new MarketPlaceAppLinks("https://groups.google.com/forum/#!forum/scmmanager", "https://www.scm-manager.com/support/", "https://www.scm-manager.com/", "https://www.scm-manager.com/scm-manager/", "http://www.scm-manager.org/", "https://bitbucket.org/sdorra/scm-manager/wiki/Home", "https://bitbucket.org/sdorra/scm-manager/src"), "false", "predefnode", "true"),
    "SugarCRM" -> MarketPlaceInput("SugarCRM", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/sugarcrm.png", "CRM", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 sugarcrm app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "ThinkUp" -> MarketPlaceInput("ThinkUp", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/thinkup.png", "Social", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 thinkup app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "Trac" -> MarketPlaceInput("Trac", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/trac.png", "BugTracking", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 trac app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "TWiki" -> MarketPlaceInput("TWiki", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/twiki.png", "Wiki", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 twiki app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "WordPress" -> MarketPlaceInput("WordPress", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/wordpress.png", "CMS", "3.8.1", "WordPress is web software you can use to create a beautiful <strong><em>website or blog</em></strong>. WordPress is both free and priceless at the same time."), "free", new MarketPlaceFeatures("Start a blog or build a full-fledged website. The only limit is your imagination. Build a great site and spend nothing, zilch, nada. And if you want to make a great site even better, a great selection of premium upgrades. Share your work with the world.", "Publicize lets you connect your WordPress site to the most popular social networks — Facebook, Twitter, Tumblr, LinkedIn, and more. Learn more about your readers, where they’re from, and how they found you. Maps and graphs that beautifully present your stats. Don’t be confined to the desk. Publish near and far with mobile apps for iPhone, iPad, Android, and BlackBerry.", "Your site is hosted on cloud across multiple data centers. That way, it’s super fast and always available. Wordpress dashboard is available in over 50 languages and counting.", "Whether you’re searching the forums, reading the support pages, you can always find a helping hand. Keep track of all your favorite blogs and discover new ones with the Reader."), new MarketPlacePlan("0", "Create 1 wordpress app", "free"), new MarketPlaceAppLinks("http://wordpress.org/support/", "#", "http://wordpress.org/", "http://codex.wordpress.org/Main_Page", "http://wordpress.com/", "http://en.wikipedia.org/wiki/WordPress", "https://github.com/WordPress/WordPress"), "false", "predefnode", "true"),
    "XWiki" -> MarketPlaceInput("XWiki", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/xwiki.png", "Wiki", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 xwiki app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"),
    "Zarafa" -> MarketPlaceInput("Zarafa", new MarketPlaceAppDetails("https://s3-ap-southeast-1.amazonaws.com/megampub/images/market_place_images/zarafa.png", "Email", "0", ""), "free", new MarketPlaceFeatures("", "", "", ""), new MarketPlacePlan("0", "Create 1 zarafa app", "free"), new MarketPlaceAppLinks("#", "#", "#", "#", "#", "#", "#"), "false", "predefnode", "false"))

  val toStream = toMap.keySet.toStream

}

case class MarketPlaceResult(id: String, name: String, appdetails: MarketPlaceAppDetails, pricetype: String, features: MarketPlaceFeatures, plan: MarketPlacePlan, applinks: MarketPlaceAppLinks, attach: String, predefnode: String, approved: String, created_at: String) {

  def toJValue: JValue = {
    import net.liftweb.json.scalaz.JsonScalaz.toJSON
    import models.json.MarketPlaceResultSerialization
    val preser = new MarketPlaceResultSerialization()
    toJSON(this)(preser.writer)
  }

  def toJson(prettyPrint: Boolean = false): String = if (prettyPrint) {
    pretty(render(toJValue))
  } else {
    compactRender(toJValue)
  }
}

object MarketPlaceResult {

  def fromJValue(jValue: JValue)(implicit charset: Charset = UTF8Charset): Result[MarketPlaceResult] = {
    import net.liftweb.json.scalaz.JsonScalaz.fromJSON
    import models.json.MarketPlaceResultSerialization
    val preser = new MarketPlaceResultSerialization()
    fromJSON(jValue)(preser.reader)
  }

  def fromJson(json: String): Result[MarketPlaceResult] = (Validation.fromTryCatch {
    parse(json)
  } leftMap { t: Throwable =>
    UncategorizedError(t.getClass.getCanonicalName, t.getMessage, List())
  }).toValidationNel.flatMap { j: JValue => fromJValue(j) }

}

object MarketPlaces {

  implicit val formats = DefaultFormats
  private def riak: GSRiak = GSRiak(MConfig.riakurl, "marketplaces")
  implicit def MarketPlacesSemigroup: Semigroup[MarketPlaceResults] = Semigroup.instance((f1, f2) => f1.append(f2))

  val metadataKey = "MarketPlace"
  val metadataVal = "MarketPlaces Creation"
  val bindex = BinIndex.named("marketplace")

  /**
   * A private method which chains computation to make GunnySack when provided with an input json, email.
   * parses the json, and converts it to nodeinput, if there is an error during parsing, a MalformedBodyError is sent back.
   * After that flatMap on its success and the account id information is looked up.
   * If the account id is looked up successfully, then yield the GunnySack object.
   */
  private def mkGunnySack(email: String, input: String): ValidationNel[Throwable, Option[GunnySack]] = {
    play.api.Logger.debug(("%-20s -->[%s]").format("models.MarketPlaces", "mkGunnySack:Entry"))
    play.api.Logger.debug(("%-20s -->[%s]").format("email", email))
    play.api.Logger.debug(("%-20s -->[%s]").format("json", input))

    val marketPlaceInput: ValidationNel[Throwable, MarketPlaceInput] = (Validation.fromTryCatch {
      parse(input).extract[MarketPlaceInput]
    } leftMap { t: Throwable => new MalformedBodyError(input, t.getMessage) }).toValidationNel //capture failure

    for {
      mkp <- marketPlaceInput
      //TO-DO: Does the leftMap make sense ? To check during function testing, by removing it.
      uir <- (UID(MConfig.snowflakeHost, MConfig.snowflakePort, "mkp").get leftMap { ut: NonEmptyList[Throwable] => ut })
    } yield {
      //TO-DO: do we need a match for None on aor, and uir (confirm it during function testing).
      val bvalue = Set(mkp.name)
      val json = new MarketPlaceResult(uir.get._1 + uir.get._2, mkp.name, mkp.appdetails, mkp.pricetype, mkp.features, mkp.plan, mkp.applinks, mkp.attach, mkp.predefnode, mkp.approved, Time.now.toString).toJson(false)
      new GunnySack(mkp.name, json, RiakConstants.CTYPE_TEXT_UTF8, None,
        Map(metadataKey -> metadataVal), Map((bindex, bvalue))).some
    }
  }

  private def mkGunnySack_init(input: MarketPlaceInput): ValidationNel[Throwable, Option[GunnySack]] = {
    play.api.Logger.debug("models.MarketPlaces mkGunnySack: entry:\n" + input.json)
    val marketplaceInput: ValidationNel[Throwable, MarketPlaceInput] = (Validation.fromTryCatch {
      parse(input.json).extract[MarketPlaceInput]
    } leftMap { t: Throwable => new MalformedBodyError(input.json, t.getMessage) }).toValidationNel //capture failure

    for {
      mkp <- marketplaceInput
      //TO-DO: Does the leftMap make sense ? To check during function testing, by removing it.
      uir <- (UID(MConfig.snowflakeHost, MConfig.snowflakePort, "mkp").get leftMap { ut: NonEmptyList[Throwable] => ut })
    } yield {
      //TO-DO: do we need a match for uir to filter the None case. confirm it during function testing.
      play.api.Logger.debug("models.marketplaces mkGunnySack: yield:\n" + (uir.get._1 + uir.get._2))
      val bvalue = Set(mkp.name)
      val mkpJson = new MarketPlaceResult(uir.get._1 + uir.get._2, mkp.name, mkp.appdetails, mkp.pricetype, mkp.features, mkp.plan, mkp.applinks, mkp.attach, mkp.predefnode, mkp.approved, Time.now.toString).toJson(false)
      new GunnySack(mkp.name, mkpJson, RiakConstants.CTYPE_TEXT_UTF8, None,
        Map(metadataKey -> metadataVal), Map((bindex, bvalue))).some
    }
  }

  /*
   * create new market place item with the 'name' of the item provide as input.
   * A index name marketplace name will point to the "marketplaces" bucket
   */
  def create(email: String, input: String): ValidationNel[Throwable, Option[MarketPlaceResult]] = {
    play.api.Logger.debug(("%-20s -->[%s]").format("models.MarketPlaces", "create:Entry"))
    play.api.Logger.debug(("%-20s -->[%s]").format("email", email))
    play.api.Logger.debug(("%-20s -->[%s]").format("json", input))

    (mkGunnySack(email, input) leftMap { err: NonEmptyList[Throwable] =>
      new ServiceUnavailableError(input, (err.list.map(m => m.getMessage)).mkString("\n"))
    }).toValidationNel.flatMap { gs: Option[GunnySack] =>
      (riak.store(gs.get) leftMap { t: NonEmptyList[Throwable] => t }).
        flatMap { maybeGS: Option[GunnySack] =>
          maybeGS match {
            case Some(thatGS) => (parse(thatGS.value).extract[MarketPlaceResult].some).successNel[Throwable]
            case None => {
              play.api.Logger.warn(("%-20s -->[%s]").format("MarketPlace.created success", "Scaliak returned => None. Thats OK."))
              (parse(gs.get.value).extract[MarketPlaceResult].some).successNel[Throwable];
            }
          }
        }
    }
  }

  def marketplace_init: ValidationNel[Throwable, MarketPlaceResults] = {
    play.api.Logger.debug("models.MarketPlaces create: entry")
    (MarketPlaceInput.toMap.some map {
      _.map { p =>
        (mkGunnySack_init(p._2) leftMap { t: NonEmptyList[Throwable] => t
        }).flatMap { gs: Option[GunnySack] =>
          (riak.store(gs.get) leftMap { t: NonEmptyList[Throwable] => t }).
            flatMap { maybeGS: Option[GunnySack] =>
              maybeGS match {
                case Some(thatGS) => MarketPlaceResults(parse(thatGS.value).extract[MarketPlaceResult]).successNel[Throwable]
                case None => {
                  play.api.Logger.warn(("%-20s -->[%s]").format("MarketPlaces.created success", "Scaliak returned => None. Thats OK."))
                  MarketPlaceResults(MarketPlaceResult(new String(), p._2.name, p._2.appdetails, p._2.pricetype, p._2.features, p._2.plan, p._2.applinks, p._2.attach, p._2.predefnode, p._2.approved, new String())).successNel[Throwable]
                }
              }
            }
        }
      }
    } map {
      _.fold((MarketPlaceResults.empty).successNel[Throwable])(_ +++ _) //fold or foldRight ? 
    }).head //return the folded element in the head.  

  }

  def findByName(marketPlacesNameList: Option[Stream[String]]): ValidationNel[Throwable, MarketPlaceResults] = {
    play.api.Logger.debug(("%-20s -->[%s]").format("models.MarketPlaces", "findByNodeName:Entry"))
    play.api.Logger.debug(("%-20s -->[%s]").format("marketPlaceList", marketPlacesNameList))
    (marketPlacesNameList map {
      _.map { marketplacesName =>
        play.api.Logger.debug("models.MarketPlaceName findByName: marketplaces:" + marketplacesName)
        (riak.fetch(marketplacesName) leftMap { t: NonEmptyList[Throwable] =>
          new ServiceUnavailableError(marketplacesName, (t.list.map(m => m.getMessage)).mkString("\n"))
        }).toValidationNel.flatMap { xso: Option[GunnySack] =>
          xso match {
            case Some(xs) => {
              (Validation.fromTryCatch {
                parse(xs.value).extract[MarketPlaceResult]
              } leftMap { t: Throwable =>
                new ResourceItemNotFound(marketplacesName, t.getMessage)
              }).toValidationNel.flatMap { j: MarketPlaceResult =>
                Validation.success[Throwable, MarketPlaceResults](nels(j.some)).toValidationNel //screwy kishore, every element in a list ? 
              }
            }
            case None => Validation.failure[Throwable, MarketPlaceResults](new ResourceItemNotFound(marketplacesName, "")).toValidationNel
          }
        }
      } // -> VNel -> fold by using an accumulator or successNel of empty. +++ => VNel1 + VNel2
    } map {
      _.foldRight((MarketPlaceResults.empty).successNel[Throwable])(_ +++ _)
    }).head //return the folded element in the head. 

  }

  def listAll: ValidationNel[Throwable, MarketPlaceResults] = {
    play.api.Logger.debug(("%-20s -->[%s]").format("models.MarketPlace", "listAll:Entry"))
    findByName(MarketPlaceInput.toStream.some) //return the folded element in the head.  
  }

  implicit val sedimentPredefResults = new Sedimenter[ValidationNel[Error, MarketPlaceResults]] {
    def sediment(maybeASediment: ValidationNel[Error, MarketPlaceResults]): Boolean = {
      val notSed = maybeASediment.isSuccess
      play.api.Logger.debug("%-20s -->[%s]".format("|^/^|-->MKP:sediment:", notSed))
      notSed
    }
  }

}