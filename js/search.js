// When the user clicks on the search box, we want to toggle the search dropdown
function displayToggleSearch(e) {
  e.preventDefault();
  e.stopPropagation();

  closeDropdownSearch(e);
  
  if (idx === null) {
    console.log("Building search index...");
    prepareIdxAndDocMap();
    console.log("Search index built.");
  }
  const dropdown = document.querySelector("#search-dropdown-content");
  if (dropdown) {
    if (!dropdown.classList.contains("show")) {
      dropdown.classList.add("show");
    }
    document.addEventListener("click", closeDropdownSearch);
    document.addEventListener("keydown", searchOnKeyDown);
    document.addEventListener("keyup", searchOnKeyUp);
  }
}

//We want to prepare the index only after clicking the search bar
var idx = null
const docMap = new Map()

function prepareIdxAndDocMap() {
  const docs = [    
    {
      "title": "Recipes",
      "url": "/deployment/recipe/",
      "content": "Gitlab CI recipe This recipe assumes that you have a namespace per application. In each namespace you can deploy a service on either test or production mode (or you can make up additional modes). It also takes into account that you may have another http service dependency. Your build.sbt can have the settings below: import kubeyml.deployment._ import kubeyml.deployment.api._ import kubeyml.deployment.plugin.Keys._ lazy val deploymentName = sys.env.getOrElse(\"DEPLOYMENT_NAME\", \"myservice-test\") lazy val secretsName = sys.env.getOrElse(\"SECRETS_NAME\", \"myservice-test-secrets\") lazy val serviceDependencyConnection = sys.env.getOrElse(\"MY_DEPENDENCY\", \"http://another-service.another-namespace:8080\") lazy val deploymentSettings = Seq( kube / namespace := \"my-namespace\", //default is name in thisProject kube / application := deploymentName, //default is name in thisProject kube / envs := Map( EnvName(\"JAVA_OPTS\") -&gt; EnvRawValue(\"-Xms256M -Xmx2048M\"), EnvName(\"MY_DEPENDENCY_SERVICE\") -&gt; EnvRawValue(serviceDependencyConnection), EnvName(\"MY_SECRET_TOKEN\") -&gt; EnvSecretValue(name = secretsName, key = \"my-token\") ), kube / resourceLimits := Resource(Cpu.fromCores(2), Memory(2048+512)), kube / resourceRequests := Resource(Cpu(500), Memory(512)), //if you want you can use something like the below to modify any part of the deployment by hand kube / deployment:= (kube / deployment).value.pullDockerImage(IfNotPresent) ) And your gitlab ci plan can look like stages: - publish-image - deploy .publish-template: stage: publish-image script: - sbt docker:publish - sbt kubeyml:gen artifacts: untracked: false paths: - target/kubeyml/deployment.yml .deploy-template: stage: deploy image: docker-image-that-has-your-kubectl-config script: - kubectl apply -f target/kubeyml/deployment.yml publish-test: before_script: export MY_DEPENDENCY=${MY_TEST_DEPENDENCY} extends: .publish-template deploy-test: extends: .deploy-template dependencies: - publish-test publish-prod: before_script: - export MY_DEPENDENCY=${MY_PROD_DEPENDENCY} - export SECRETS_NAME=${MY_PROD_SECRET_NAME} - export DEPLOYMENT_NAME=my-service-prod extends: .publish-template deploy-prod: extends: .deploy-template dependencies: - publish-prod"
    } ,    
    {
      "title": "Deployment manifest",
      "url": "/deployment/",
      "content": "Deployment Deployment generates a deployment.yml file as specified in https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#creating-a-deployment The API is a bit opinionated from experiences in production and it forces you to set a liveness probe and a readiness probe. It also sets defaults on resources but you can customise your own. Because deployment manifests can be quite big there is a DSL so you can create your own. The below import kubeyml.deployment._ import kubeyml.deployment.api._ import scala.concurrent.duration._ deploy.namespace(\"yournamespace\") .service(\"nginx-deployment\") .withImage(\"nginx:1.7.9\") .withProbes( livenessProbe = HttpProbe(HttpGet(\"/\", port = 80, httpHeaders = List.empty), period = 10 seconds), readinessProbe = HttpProbe(HttpGet(\"/\", port = 80, httpHeaders = List.empty), failureThreshold = 10) ).replicas(3) .pullDockerImage(IfNotPresent) .addPorts(List(Port(None, 80))) .deploymentStrategy(RollingUpdate(0, 1)/*Or Recreate*/) Would generate this yaml file kind: Deployment apiVersion: apps/v1 metadata: name: nginx-deployment namespace: yournamespace spec: replicas: 3 selector: matchLabels: app: nginx-deployment template: metadata: labels: app: nginx-deployment annotations: {} spec: containers: - imagePullPolicy: IfNotPresent ports: - containerPort: 80 image: nginx:1.7.9 readinessProbe: httpGet: path: / port: 80 timeoutSeconds: 1 periodSeconds: 10 initialDelaySeconds: 0 successThreshold: 1 failureThreshold: 10 name: nginx-deployment livenessProbe: timeoutSeconds: 1 failureThreshold: 3 httpGet: path: / port: 80 initialDelaySeconds: 0 successThreshold: 1 periodSeconds: 10 resources: requests: cpu: 100m memory: 256Mi limits: cpu: 1000m memory: 512Mi strategy: type: RollingUpdate rollingUpdate: maxSurge: 0 maxUnavailable: 1 Sbt Properties On top of all this, there is an sbt layer which allows you to interact exclusively with sbt keys. sbt key description default namespace The kubernetes namespace of the deployment Default value is project name application The name of the deployment Default value is project name dockerImage The docker image to deploy in a single container Default is the picked from sbt-native-packager ports List of container ports optionally tagged with name dockerExposedPorts from docker plugin livenessProbe Healthcheck probe HttpProbe(HttpGet(\"/health\", 8080, List.empty), 0 seconds, 1 second, 10 seconds, 3, 1) readinessProbe Probe to check when deployment is ready to receive traffic livenessProbe annotations Map[String, String] for spec template annotations (e.g. aws roles) empty replicas the number of replicas to be deployed 2 imagePullPolicy Image pull policy for kubernetes, set to IfNotPresent or Always IfNotPresent command Command for the container empty args arguments for the command empty Seq envs Map of environment variables, raw, field path or secret are supported empty resourceRequests Resource requests (cpu in the form of m, memory in the form of MiB Resource(Cpu(100), Memory(256)) resourceLimits Resource limits (cpu in the form of m, memory in the form of MiB Resource(Cpu(1000), Memory(512)) target The directory to output the deployment.yml target of this project deployment The key to access the whole Deployment definition, exposed for further customisation Instance with above defaults persistentVolumes Persistent volumes that should be used by deployment pods empty Seq Go to the deployment recipe for a full deployment example."
    } ,    
    {
      "title": "Recipes",
      "url": "/ingress/recipe/",
      "content": "Gitlab CI recipe It is recommended that you set a custom ingress name despite the fact that it is derived from the service name. You also need to set a host and the rules against the service ports (there is no dsl here to help with programmatic mapping) But if you make a mistake and you use a service name or a port that doesn’t match, you’ll get an error with details on what’s going wrong. enablePlugins(KubeIngressPlugin) lazy val ingressEnvName = sys.env.getOrElse(\"HELLO_INGRESS_NAME\", \"helloworld-ingress-test\") lazy val hostName = sys.env.getOrElse(\"YOUR_HOST_NAME\", \"your-hostname.yourdomain.smth\") import kubeyml.protocol.NonEmptyString import kubeyml.protocol.Host import kubeyml.deployment.plugin.Keys._ import kubeyml.ingress.api._ import kubeyml.ingress.plugin.Keys._ import kubeyml.service.plugin.Keys._ import kubeyml.ingress.plugin.Keys._ import kubeyml.ingress.{HttpRule, ServiceMapping, Path =&gt; IngressPath} val ingressSettings = Seq( (kube / ingressName) := ingressEnvName, (kube / ingressRules) := List( HttpRule(Host(hostName), List( IngressPath(ServiceMapping((kube / service).value.name, 8085), \"/hello-world\") )) ), (kube / ingressAnnotations) := Map( Annotate.nginxIngress(), // this adds kubernetes.io/ingress.class: nginx Annotate.nginxRewriteTarget(\"/hello-world\"), //this adds nginx.ingress.kubernetes.io/rewrite-target: /hello-world NonEmptyString(\"your-own-annotation-key\") -&gt; \"value\" ) ) The command is the same kubeyml:gen And will generate an ingress.yml file Now you can extend your gitlab ci to look like the below. .publish-template: stage: publish-image script: - sbt docker:publish - sbt kubeyml:gen artifacts: untracked: true paths: - target/kubeyml/deployment.yml - target/kubeyml/service.yml - target/kubeyml/ingress.yml .deploy-template: stage: deploy image: docker-image-that-has-your-kubectl-config script: - kubectl apply -f target/kubeyml/deployment.yml - kubectl apply -f target/kubeyml/service.yml - kubectl apply -f target/kubeyml/ingress.yml"
    } ,    
    {
      "title": "Ingress manifest",
      "url": "/ingress/",
      "content": "Ingress This generates an ingress as defined here The ingress derives some properties from the service but it requires you to set the hostname. If we continue with our nginx example from the service section, the ingress looks like this import kubeyml.protocol.Host import kubeyml.ingress.{CustomIngress, HttpRule, Ingress, ServiceMapping, Path =&gt; IngressPath, Spec =&gt; IngressSpec} import kubeyml.ingress.api.Annotate import kubeyml.deployment.{HttpProbe, IfNotPresent, HttpGet, Port =&gt; DeployPort} import kubeyml.service._ import kubeyml.deployment.api._ import scala.concurrent.duration._ val deployment = deploy.namespace(\"yournamespace\") .service(\"nginx-deployment\") .withImage(\"nginx:1.7.9\") .withProbes( livenessProbe = HttpProbe(HttpGet(\"/\", port = 80, httpHeaders = List.empty), period = 10 seconds), readinessProbe = HttpProbe(HttpGet(\"/\", port = 80, httpHeaders = List.empty), failureThreshold = 10) ).replicas(3) .pullDockerImage(IfNotPresent) .addPorts(List(DeployPort(None, 80)) ) val service = Service.fromDeployment(deployment) val ingress: Ingress = CustomIngress( \"nginx-ingress\", \"yournamespace\", Map(Annotate.nginxRewriteTarget(\"/\"), Annotate.nginxIngress()), IngressSpec( List(HttpRule(Host(\"your-host.domain.smth\"), List(IngressPath(ServiceMapping(\"nginx-deployment\", 80), \"/testpath\")))) ) ) Since version 0.2.9, ingress uses the new API version following this migration guide This would generate the following yaml file apiVersion: networking.k8s.io/v1beta1 kind: Ingress metadata: annotations: nginx.ingress.kubernetes.io/rewrite-target: / kubernetes.io/ingress.class: nginx name: nginx-ingress namespace: yournamespace spec: rules: - host: your-host.domain.smth http: paths: - backend: serviceName: nginx-deployment servicePort: 80 path: /testpath To switch to the legacy apiVersion simply do: def toExtensions_v1beta1(customIngress: CustomIngress) = customIngress.legacy Of course in most cases you won’t have to write any of this as the sbt properties that sit on top of the API are derived from the service plugin which this depends upon. See the recipe for an example. Sbt properties sbt key description default ingressRules A list of Rules (currently only supports HttpRule N/A ingressName The name of the ingress The application name from deployment ingress Key configuration for modifying the ingress properties Some values are derived from service"
    } ,    
    {
      "title": "Recipes",
      "url": "/akka-cluster/recipe/",
      "content": "Akka cluster configuration with Akka management You may have the configuration below in you application.conf akka { discovery.kubernetes-api { # Namespace discovery path # # If this path doesn't exist, the namespace will default to \"default\". # Namespace to query for pods. # # Set this value to a specific string to override discovering the namespace using pod-namespace-path. pod-namespace = ${?MY_NAMESPACE} # Selector value to query pod API with. # `%s` will be replaced with the configured effective name, which defaults to the actor system name pod-label-selector = \"app=%s\" } remote.artery { canonical { hostname = \"127.0.0.1\" hostname = ${?MY_HOSTNAME} port = 2551 } } } akka.management { cluster.bootstrap { contact-point-discovery { discovery-method = kubernetes-api } } } akka.management.http.port = 8558 akka.management.http.hostname = ${?MY_HOSTNAME} For the discovery to work, the hostname needs to be the podIp. By enabling this plugin you don’t need to have this configuration in the deployment. Example import kubeyml.deployment._ import kubeyml.deployment.api._ import kubeyml.deployment.plugin.Keys._ import kubeyml.roles.akka.cluster.plugin.Keys._ lazy val akkaClusterKubernetesSettings = Seq( kube / hostnameEnv := Some(EnvName(\"MY_HOSTNAME\")), kube / namespaceEnv := Some(EnvName(\"MY_NAMESPACE\")) ) lazy val deploymentSettings = Seq( kube / namespace := \"my-namespace\", kube / application := \"my-application\", kube / envs ++= Map( EnvName(\"ANOTHER_ENV\") -&gt; EnvRawValue(\"something\") ), kube / resourceRequests := Resource(Cpu.fromCores(1), Memory(512)), kube / resourceLimits := Resource(Cpu.fromCores(2), Memory(2048 + 256)) ) Notice that you don’t need to specify the liveness probe and the readiness probe. The AkkaClusterPlugin configures the following probes: kube / livenessProbe := HttpProbe(HttpGet(\"/alive\", 8558, List.empty), 10 seconds, 3 seconds, 5 seconds), kube / readinessProbe := HttpProbe(HttpGet(\"/ready\", 8558, List.empty), 10 seconds, 3 seconds, 5 seconds), The plugin only depends on the deployment plugin. If you need this exposed via an ingress you need to configure that yourself. Gitlab integration Following the example in deployment you can modify the templates as below. .publish-template: stage: publish-image script: - sbt kubeyml:gen artifacts: untracked: false paths: - target/kubeyml/ .deploy-template: stage: deploy image: docker-image-that-has-your-kubectl-config script: - kubectl apply -f target/kubeyml/deployment.yml - kubectl apply -f target/kubeyml/roles.yml"
    } ,    
    {
      "title": "Settings",
      "url": "/akka-cluster/settings/",
      "content": "Plugin configuration Depending on the configuration you use you may need to set the names for the following environment variables. sbt key description default discoveryMethodEnv The environment variable name that controls the discovery method. The value will be set to kubernetes-api None hostnameEnv The environment variable name that controls the hostname. This will be set into a kubernetes field value (status.podIP) None namespaceEnv The environment variable name that controls the namespace. This will be set to the namespace defined in the deployment None If you don’t set this values, the plugin assume that you have set them manually in the deployment settings and it won’t attempt to define them. Remember that these are for the environment variable names not the values. The values are set automatically as specified in the Akka documentation for kubernetes api discovery here The generated yaml file can contain the following environment variables env: - name: MY_DISCOVERY_METHOD value: kubernetes-api - name: MY_HOSTNAME valueFrom: fieldRef: fieldPath: status.podIP - name: AKKA_CLUSTER_BOOTSTRAP_SERVICE_NAME valueFrom: fieldRef: fieldPath: metadata.labels['app'] The AKKA_CLUSTER_BOOTSTRAP_SERVICE_NAME environment variable is added by default. You can only control the 3 environment variables mentioned in the table above."
    } ,    
    {
      "title": "Akka Cluster",
      "url": "/akka-cluster/",
      "content": "Akka cluster support Akka management supports cluster bootstrapping via the Kubernetes API This may require several configurations in application.conf and Kubernetes manifests to get it working. The plugin provides the ability to auto-generate the roles yaml file and also inject the deployment with environment variables of the user’s choice. See the recipes section for examples and the settings for explanation on the available options. Alternatively you can get started by enabling the plugin and let the API lead you. enablePlugins(KubeDeploymentPlugin, AkkaClusterPlugin) kubeyml:gen"
    } ,    
    {
      "title": "Recipes",
      "url": "/service/recipe/",
      "content": "Enable the plugin in your module that you want to deploy enablePlugins(KubeServicePlugin) The command is the same kubeyml:gen Then your gitlab publish template will look like (example extended from deployment recipe) .publish-template: stage: publish-image script: - sbt docker:publish - sbt kubeyml:gen artifacts: untracked: true paths: - target/kubeyml/deployment.yml - target/kubeyml/service.yml And deploy with .deploy-template: stage: deploy image: docker-image-that-has-your-kubectl-config script: - kubectl apply -f target/kubeyml/deployment.yml - kubectl apply -f target/kubeyml/service.yml"
    } ,    
    {
      "title": "Service manifest",
      "url": "/service/",
      "content": "Service This generates a service manifest as described here https://kubernetes.io/docs/concepts/services-networking/service/#defining-a-service Generating a service is very simple and normally you just need to enable the service plugin. The service plugin depends on the deployment plugin and thus, every property of the service is automatically derived from the deployment definition. Consider the nginx deployment from previous, we can fully derive the service: import kubeyml.deployment._ import kubeyml.service._ import kubeyml.deployment.api._ import scala.concurrent.duration._ val deployment = deploy.namespace(\"yournamespace\") .service(\"nginx-deployment\") .withImage(\"nginx:1.7.9\") .withProbes( livenessProbe = HttpProbe(HttpGet(\"/\", port = 80, httpHeaders = List.empty), period = 10 seconds), readinessProbe = HttpProbe(HttpGet(\"/\", port = 80, httpHeaders = List.empty), failureThreshold = 10) ).replicas(3) .pullDockerImage(IfNotPresent) .addPorts(List(Port(None, 80)) ) val service = Service.fromDeployment(deployment) Which would generate the following service.yml file kind: Service apiVersion: v1 metadata: name: nginx-deployment namespace: yournamespace spec: type: NodePort selector: app: nginx-deployment ports: - name: pn80 protocol: TCP port: 80 targetPort: 80 At the moment there’s little room for customisation. If you have a use-case you can open an issue or even better a pull request. Sbt Properties These are the available properties that you can customise from the sbt layer. sbt key description default portMappings Port mappings against the deployment (service to pod) Derived from deployment service Key configuration for modifying the service properties Derived from deployment You can also create your own instance of the service case class and see how far customisation can get you. There’s no DSL for this yet. Go to the service recipe for a full deployment example."
    } ,    
    {
      "title": "Helm Support",
      "url": "/helm/",
      "content": "Helm support This is an experimental feature It is possible since 0.3.9 to generate a helm compatible structure. You have to simply enable HelmPlugin enablePlugins(KubeDeploymentPlugin, KubeServicePlugin, KubeIngressPlugin, HelmPlugin) This will generate under target/kubeyml a Chart.yml and a templates/ directory with all the kubernetes manifests already rendered. The aim for this is that you can have the whole templating done with Scala in your sbt build configuration. Mixing the file If you want to have a hybrid solution you can mix helm templates and this plugin. In order to make the plugin store the files somewhere other than target/kubeyml you can override the setting. (kube / target) := \".deployment/templates\" Remember that if you override the target you need to point to a templates directory"
    } ,        
  ];

  idx = lunr(function () {
    this.ref("title");
    this.field("content");

    docs.forEach(function (doc) {
      this.add(doc);
    }, this);
  });

  docs.forEach(function (doc) {
    docMap.set(doc.title, doc.url);
  });
}

// The onkeypress handler for search functionality
function searchOnKeyDown(e) {
  const keyCode = e.keyCode;
  const parent = e.target.parentElement;
  const isSearchBar = e.target.id === "search-bar";
  const isSearchResult = parent ? parent.id.startsWith("result-") : false;
  const isSearchBarOrResult = isSearchBar || isSearchResult;

  if (keyCode === 40 && isSearchBarOrResult) {
    // On 'down', try to navigate down the search results
    e.preventDefault();
    e.stopPropagation();
    selectDown(e);
  } else if (keyCode === 38 && isSearchBarOrResult) {
    // On 'up', try to navigate up the search results
    e.preventDefault();
    e.stopPropagation();
    selectUp(e);
  } else if (keyCode === 27 && isSearchBarOrResult) {
    // On 'ESC', close the search dropdown
    e.preventDefault();
    e.stopPropagation();
    closeDropdownSearch(e);
  }
}

// Search is only done on key-up so that the search terms are properly propagated
function searchOnKeyUp(e) {
  // Filter out up, down, esc keys
  const keyCode = e.keyCode;
  const cannotBe = [40, 38, 27];
  const isSearchBar = e.target.id === "search-bar";
  const keyIsNotWrong = !cannotBe.includes(keyCode);
  if (isSearchBar && keyIsNotWrong) {
    // Try to run a search
    runSearch(e);
  }
}

// Move the cursor up the search list
function selectUp(e) {
  if (e.target.parentElement.id.startsWith("result-")) {
    const index = parseInt(e.target.parentElement.id.substring(7));
    if (!isNaN(index) && (index > 0)) {
      const nextIndexStr = "result-" + (index - 1);
      const querySel = "li[id$='" + nextIndexStr + "'";
      const nextResult = document.querySelector(querySel);
      if (nextResult) {
        nextResult.firstChild.focus();
      }
    }
  }
}

// Move the cursor down the search list
function selectDown(e) {
  if (e.target.id === "search-bar") {
    const firstResult = document.querySelector("li[id$='result-0']");
    if (firstResult) {
      firstResult.firstChild.focus();
    }
  } else if (e.target.parentElement.id.startsWith("result-")) {
    const index = parseInt(e.target.parentElement.id.substring(7));
    if (!isNaN(index)) {
      const nextIndexStr = "result-" + (index + 1);
      const querySel = "li[id$='" + nextIndexStr + "'";
      const nextResult = document.querySelector(querySel);
      if (nextResult) {
        nextResult.firstChild.focus();
      }
    }
  }
}

// Search for whatever the user has typed so far
function runSearch(e) {
  if (e.target.value === "") {
    // On empty string, remove all search results
    // Otherwise this may show all results as everything is a "match"
    applySearchResults([]);
  } else {
    const tokens = e.target.value.split(" ");
    const moddedTokens = tokens.map(function (token) {
      // "*" + token + "*"
      return token;
    })
    const searchTerm = moddedTokens.join(" ");
    const searchResults = idx.search(searchTerm);
    const mapResults = searchResults.map(function (result) {
      const resultUrl = docMap.get(result.ref);
      return { name: result.ref, url: resultUrl };
    })

    applySearchResults(mapResults);
  }

}

// After a search, modify the search dropdown to contain the search results
function applySearchResults(results) {
  const dropdown = document.querySelector("div[id$='search-dropdown'] > .dropdown-content.show");
  if (dropdown) {
    //Remove each child
    while (dropdown.firstChild) {
      dropdown.removeChild(dropdown.firstChild);
    }

    //Add each result as an element in the list
    results.forEach(function (result, i) {
      const elem = document.createElement("li");
      elem.setAttribute("class", "dropdown-item");
      elem.setAttribute("id", "result-" + i);

      const elemLink = document.createElement("a");
      elemLink.setAttribute("title", result.name);
      elemLink.setAttribute("href", result.url);
      elemLink.setAttribute("class", "dropdown-item-link");

      const elemLinkText = document.createElement("span");
      elemLinkText.setAttribute("class", "dropdown-item-link-text");
      elemLinkText.innerHTML = result.name;

      elemLink.appendChild(elemLinkText);
      elem.appendChild(elemLink);
      dropdown.appendChild(elem);
    });
  }
}

// Close the dropdown if the user clicks (only) outside of it
function closeDropdownSearch(e) {
  // Check if where we're clicking is the search dropdown
  if (e.target.id !== "search-bar") {
    const dropdown = document.querySelector("div[id$='search-dropdown'] > .dropdown-content.show");
    if (dropdown) {
      dropdown.classList.remove("show");
      document.documentElement.removeEventListener("click", closeDropdownSearch);
    }
  }
}
