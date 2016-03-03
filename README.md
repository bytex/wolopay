# wolopay library

## Installation

You can clone the repo and then issue

    mvn install

Library will be installed to a local maven repository. Then you can use it by adding the following to your `pom.xml`:

        <dependency>
            <groupId>ru.bytexgames</groupId>
            <artifactId>wolopay</artifactId>
            <version>1.0</version>
        </dependency>

In case you are using gradle:

    compile 'ru.bytexgames:wolopay:1.0'

Or, you may just add the class ru.bytexgames.integration.Wolopay to your project.

## Adding a bean to your project

### Grails

You can instantiate the wolopay client in your resources.groovy:

```groovy
    import ru.bytexgames.integration.wolopay.Wolopay
    wolopay(Wolopay){   
      clientId = "clientId"
      secret = "secret"
      debug = true
      sandbox = false
    }
```
Of course you'll need to provide correct `clientId` and `secret` values.
It's also possible to use Grails environment for your convenience:

```groovy
wolopay(Wolopay){   
  if (Environment.current.name == 'production') {
    clientId = "Production clientId"
    secret = "Production secret"
    sandbox = false
  } else {
    clientId = "Development clientId"
    secret = "Development secret"
    sandbox = true
  }
  debug = true
}
```

### Spring bean

If you're using Spring, you can instantiate the Wolopay bean in your `application-context.xml`.

## Some examples (in Groovy language)

* Process payment notification from `Wolopay`:

Please note that: `wolopay` is autowired by Grails. Please note that: `log`, `params` and `request` are injected by Grails.

```groovy
  def wolopay // Autowired by grails
  def processWolopayNotification() {
    log.debug("Process Wolopay notification: ${params}");

    if (!wolopay.isAValidRequest(request)) {
        log.info("Invalid request, authorization was: " + request.getHeader("Authorization"));
        response.sendError(500, "Invalid response")
        return
    }

    final Player player = Player.get(params.gamerId as long)
    if (player == null) {
        log.error("Invalid payment request. Player not found: ${params.userId} ${params.gamerId}")
        render GENERAL_FAILURE
        return
    }

    // TODO: Give some money to a player in a new transaction
    // After the transaction is committed, return empty response with 200 HTTP code to the Wolopay gateway:

    render WOLOPAY_SUCCESS
  }
```
* Determine Wolopay transaction URL:
```groovy
  // You can pass some extra variables to Wolopay:
  def extra = ['gamer_email': player.email ?: '', 'fixed_country': 1, 'fixed_language': 1]
// You can also open specific tab, and select default item:  //  extra += [ selected_tab_category_id: 'subscription', 
  //             selected_article_id: '12345678-abcd-dcba-0000-000000000000' ]
  
  // You can also pass specific language:
  Locale locale = UtilLocale.resolveLocale(request);
  extra += ['default_language': locale.language]
  def wolopayUrl = wolopay.createTransactionUrl(player.id as String, player.level as String, extra)
```

