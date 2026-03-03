package org.example;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.awt.Desktop;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try (Context polyglot = Context.create()) {

            Value array = polyglot.eval("js", "['If', 'we', 'run', 'the', 'java', 'command']");

            String[] elem=new String[Math.toIntExact(array.getArraySize())];        // imi formez un fel de hashmap
            int[] nrOrd=new int[Math.toIntExact(array.getArraySize())];             // cheia e nr de ordine
            Arrays.fill(nrOrd,-1);                                              // toate nr de ord sunt initializate cu -1
                                                                                    // ca pot fi si nr de ord 0, ex: If, we

            for (int i = 0; i < array.getArraySize(); i++) {
                String element = array.getArrayElement(i).asString();
                String upper = RToUpper(element);
                int crc = SumCRC(upper);
                System.out.println(upper + " -> " + crc);

                for(int j=0;j<nrOrd.length;++j)
                {
                    if(nrOrd[j]==crc) {
                        elem[j]+=", "+element;
                        break;
                    }

                    if(nrOrd[j]==-1)
                    {
                        nrOrd[j]=crc;
                        elem[j]=element;
                        break;
                    }
                }
            }
            System.out.println("\nTema ex 1)");
            System.out.println("Elemente cu acelasi nr de ordine: ");
            for(int i=0;i<elem.length && nrOrd[i]!=-1;++i)
            {
                System.out.println(nrOrd[i]+": "+elem[i]);
            }
            System.out.println("");
        }

        // deschide o "camera" in care ruleaza alte limbaje
        try (Context context = Context.newBuilder().allowAllAccess(true).build())
        {
            // functie python care genereaza aleatoriu o lista de 20 de nr intregi
            Value pythonList = context.eval("python",
                    "import random\n" +
                            "[random.randint(0, 100) for _ in range(20)]");

            // pasez datele catre JS injectandu le intr o variabila "myList" parand ca myList este o variabila a lu JS
            context.getBindings("js").putMember("myList", pythonList);

            Value result = context.eval("js",
                    "(function(arr) {" +
                            "   let numericArray = Array.from(arr).sort((a, b) => a - b);" + // sortare
                            "   let n = numericArray.length;" +
                            "   let toRemove = Math.floor(n * 0.20);" + // calculam 20%
                            "   let trimmedArray = numericArray.slice(toRemove, n - toRemove);" + // se elimina primele si ultimele elem
                            "   let sum = trimmedArray.reduce((a, b) => a + b, 0);" +             // acumuleaza suma in a(initial 0)
                            "   console.log('Lista sortata si taiata (JS):', trimmedArray.toString());" +
                            "   return sum / trimmedArray.length;" +
                            "})(myList)");

            // rezultat final
            System.out.println("Media aritmetica a elementelor ramasa dupa taiere: " + result.asDouble());
        }



        // ex 2 din tema
        System.out.println("\nTema ex 2)");
        Scanner scanner = new Scanner(System.in);

        System.out.print("Introdu directorul de salvare (ex: . pentru folderul curent): ");
        String path = scanner.nextLine();

        System.out.print("Introdu numele imaginii (obligatoriu cu extensia .svg, ex: grafic.svg): ");
        String fileName = scanner.nextLine();

        System.out.print("Introdu culoarea liniei de regresie (ex: red, blue, green): ");
        String color = scanner.nextLine();

        // citim din dataset datele
        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        try (Scanner fileScanner = new Scanner(new File("dataset.txt"))) {
            while (fileScanner.hasNextDouble()) {
                xList.add(fileScanner.nextDouble());
                yList.add(fileScanner.nextDouble());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Eroare: Fisierul dataset.txt nu a fost gasit!");
            return;
        }

        // logica regresie liniara:
        // ecuatia dreptei: Y = m*X + b    unde     m=panta     b=punctul de intersectie a dreptei cu axa Y
        // formulele de aflare a pantei si punctului de intersectie:
        // m=[ n*suma(X*Y) - suma(X)*suma(Y) ] / [ n*suma(X^2) - (suma(X))^2 ]
        // b = [ suma(Y) - m*suma(X) ] / n



        // imaginea este o imagine vectoriala <svg> unde fiecare punct este generat ca un cerc <circle> si dreapta calculata ca o linie <line>
        // logica regresie + genererare imagine
        try (Context context = Context.newBuilder().allowAllAccess(true).build()) {
            // pasam iar datele spre JS
            context.getBindings("js").putMember("xData", xList.toArray());
            context.getBindings("js").putMember("yData", yList.toArray());
            context.getBindings("js").putMember("plotColor", color);

            String jsCode = "(function(x, y, color) {\n" +
                    "    let n = x.length;\n" +
                    "    let sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;\n" +
                    "    for (let i = 0; i < n; i++) {\n" +
                    "        sumX += x[i]; sumY += y[i];\n" +
                    "        sumXY += x[i] * y[i]; sumXX += x[i] * x[i];\n" +
                    "    }\n" +
                    "    let m = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);\n" +
                    "    let b = (sumY - m * sumX) / n;\n" +
                    "    let svg = `<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"600\" height=\"600\" style=\"background: #f4f4f4; border: 1px solid black;\">`;\n" +
                    "    for(let i = 0; i < n; i++) {\n" +
                    "       svg += `<circle cx=\"${x[i] * 10 + 50}\" cy=\"${550 - y[i] * 10}\" r=\"4\" fill=\"black\" />`;\n" +
                    "    }\n" +
                    "    let y1 = m * 0 + b;\n" +
                    "    let y2 = m * 50 + b;\n" +
                    "    svg += `<line x1=\"50\" y1=\"${550 - y1 * 10}\" x2=\"550\" y2=\"${550 - y2 * 10}\" stroke=\"${color}\" stroke-width=\"3\" />`;\n" +
                    "    svg += `</svg>`;\n" +
                    "    return { slope: m, intercept: b, imageStr: svg };\n" +
                    "})(xData, yData, plotColor);";

            Value result = context.eval("js", jsCode);

            System.out.println("Ecuatia calculata de JS: Y = " + result.getMember("slope").asDouble() + " * X + " + result.getMember("intercept").asDouble());

            // salvam pe disc apeland OS u
            String svgContent = result.getMember("imageStr").asString();
            File imageFile = new File(path, fileName);
            Files.writeString(imageFile.toPath(), svgContent);

            System.out.println("Imaginea a fost salvata la: " + imageFile.getAbsolutePath());

            // deschidem imaginea cu aplicatia
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(imageFile);
            } else {
                System.out.println("Sistemul tau nu suporta deschiderea automata a fisierelor. Cauta-l manual.");
            }
        } catch (Exception e) {
            System.out.println("A aparut o eroare la salvare/deschidere: " + e.getMessage());
        }


        System.out.println("\nTema ex 3)");

        try (Context context = Context.newBuilder().allowAllAccess(true).build())
        {
            // pasez scanneru de java catre python
            context.getBindings("python").putMember("jScanner", scanner);
            String pyCode=
                    "def citeste(sc):\n" +
                            "    print('nr total aruncari(n): ', end='', flush=True)\n" +
                            "    n = int(sc.nextLine())\n" +
                            "    print('nr maxim pajuri(x): ', end='', flush=True)\n" +
                            "    x=int(sc.nextLine())\n" +
                            "    return [n, x]\n" +
                            "citeste(jScanner)";

            Value pyRez=context.eval("python", pyCode);
            int n=pyRez.getArrayElement(0).asInt();
            int x=pyRez.getArrayElement(1).asInt();

            if (x<1 || x>n)
            {
                System.out.println("Eroare: x trebuie să fie între 1 și " + n);
            }
            else
            {

                // pasam iar datele spre JS
                context.getBindings("js").putMember("valN", n);
                context.getBindings("js").putMember("valX", x);

                // logica pentru functia de repartitie binomiala
                // P(X=k) = Combinari de n luate cate k * p^k * (1-p)^(n-k)
                // P(X<=k) = suma(Comb(n,k) cu 0<=k<=x  * (0.5)^n)      0.5=prob sa fie pajura=prob sa fie cap -> 0.5^k * 0.5^(n-k) = 0.5^n

                String jsStatCode =
                        "(function(x, n) {\n" +                     // combinari de n luate cate k
                                "    function comb(n, k) {\n" +
                                "        let res = 1;\n" +
                                "        for (let i = 1; i <= k; i++) { res = res * (n - i + 1) / i; }\n" +
                                "        return res;\n" +
                                "    }\n" +                     // calculam functia de repartitie binomiala
                                "    let prob = 0;\n" +
                                "    for (let i = 0; i <= x; i++) {\n" +
                                "        prob += comb(n, i) * Math.pow(0.5, n);\n" +    // 0.5 sanse pt pajura
                                "    }\n" +
                                "    return prob;\n" +
                                "})(valX, valN);";

                Value jsResult = context.eval("js", jsStatCode);
                double probabilitate = jsResult.asDouble();

                // rezultat ca procent
                System.out.printf("Probabilitatea de a obtine de cel mult %d ori pajura din %d aruncari este: %.2f%%\n", x, n, probabilitate * 100);
            }
        } catch (Exception e) {
            System.out.println("Eroare la calculul probabilitatii: " + e.getMessage());
        }
    }

    // aici am folosit js in loc de R
    private static String RToUpper(String token) {
        try (Context polyglot = Context.newBuilder().allowAllAccess(true).build()) {
            Value result = polyglot.eval("js", "\"" + token + "\".toUpperCase()");
            return result.asString();
        }
    }

    private static int SumCRC(String token) {

        if(token!=null && token.length()>=3)
        {
            token=token.substring(1,token.length()-1);
        }
        else
        {
            token="";
        }

        try (Context polyglot = Context.newBuilder().allowAllAccess(true).build()) {
            Value result = polyglot.eval("python", "sum(ord(ch)**2+1 for ch in '" + token + "')");
            return result.asInt();
        }
    }
}