# How to deploy Examind on Kubernetes

1 - In `examind-deployment.yaml` :
Replace `CSTL_URL` and `CSTL_SERVICE_URL` by the URL you setup to access Examind.
Replace `<namespace>` by the namespace where you are going to deploy this stack.

2 - Create a PVC for each PV in this directory. The volumes sizes specified here as example, you can tweak them for your needs.

3 - This is if you want to use the private Geomatys Docker registry. Create a secret named `docker-registry-geomatys-private` in the namespace where you are going to deploy the application with the docker registry credentials.

```bash
$ kubectl -n <namespace> create secret docker-registry docker-registry-geomatys-private --docker-server=docker.geomatys.com --docker-username=<your-name> --docker-password=<your-pword> --docker-email=<your-email>
```

4 - Deploy your applications in this order :

- *.pvc.yaml
- db-deployment.yaml and dbpostgis-deployment.yaml
- examind-deployment.yaml
- *.svc.yaml

5 - If you want to access the application locally, you need to do a port-forward of the HTTP application port. Don't forget to replace `CSTL_URL` and `CSTL_SERVICE_URL` by your local adress (localhost) in the examind deployment in this case.

```bash
$ kubectl -n <namespace> get pods
NAME                                         READY     STATUS    RESTARTS   AGE
[...]
examind-examind-xxxxxxxxxx-xxxxx             1/1       Running   0          5m
[...]

$ kubectl -n <namespace> port-forward -p examind-examind-xxxxxxxxxx-xxxxx 8080:8080
```

You can access the application at this URL : localhost/examind

If you want your application to face internet, create an Ingress to be consumed by your Ingress controller. Here is an example for a Nginx Ingress controller :

```
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  annotations:
    kubernetes.io/ingress.class: nginx
  labels:
    app: examind
  name: examind-examind
  namespace: <namespace>
spec:
  rules:
  - host: <CSTL_URL>
    http:
      paths:
      - backend:
          serviceName: examind-examind
          servicePort: 8080
        path: /
```

After deploying this Ingress, you can access the application at this URL : <CSTL_URL\>/examind