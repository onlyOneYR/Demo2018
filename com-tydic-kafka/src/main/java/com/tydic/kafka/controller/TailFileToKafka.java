package com.tydic.kafka.controller;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TailFileToKafka {
    public static class TailFileThread extends Thread {
        File file;
        RandomAccessFile randomFile = null;
        String newfile = null;
        String thisfile = null;
        String prefile = null;
        private long lastTimeFileSize = 0;
        private String drname = null;
        //控制记录当前读取行的频率
        int ln = 0;
        //控制一万条sleep一次，缓解压力
        int ln_s = 0;
        int beginln = 0;
        private Producer<String, String> inner;
        java.util.Random ran = new Random();
        String topicname = null;
        String fileregex = "";
        int writepostion_rowcnt;
        int pause_rouwcnt;

        public TailFileThread(String path, String drname, String topicname, String fileregex, int writepostion_rowcnt, int pause_rouwcnt) throws FileNotFoundException, IOException {
            file = new File(path);
            this.drname = drname;
            this.topicname = topicname;
            this.fileregex = fileregex;
            Properties properties = new Properties();
            properties.load(new FileInputStream("conf/producer.properties"));
            ProducerConfig config = new ProducerConfig(properties);
            inner = new KafkaProperties.Producer<String, String>(config);
            this.writepostion_rowcnt = writepostion_rowcnt;
            this.pause_rouwcnt = pause_rouwcnt;
        }

        public void send(String topicName, String message) {
            if (topicName == null || message == null) {
                return;
            }
            //      KeyedMessage<String, String> km = new KeyedMessage<String, String>(topicName,message.substring(0, 7),message);
            KeyedMessage<String, String> km = new KeyedMessage<String, String>(topicName, message, message);

            inner.send(km);
            km = null;

        }

        public void send(String topicName, Collection<String> messages) {
            if (topicName == null || messages == null) {
                return;
            }
            if (messages.isEmpty()) {
                return;
            }
            //   List<KeyedMessage<String, String>> kms = new ArrayList<KeyedMessage<String, String>>();
            List<KeyedMessage<String, String>> kms = new ArrayList<KeyedMessage<String, String>>();
            for (String entry : messages) {
                //  	KeyedMessage<String, String> km = new KeyedMessage<String,String>(topicName,entry);
                KeyedMessage<String, String> km = new KeyedMessage<String, String>(topicName, entry);
                kms.add(km);
            }
            inner.send(kms);
        }

        public void close() {
            inner.close();
        }

        public String getNewFile(File file) {
            File[] fs = file.listFiles();
            long maxtime = 0;
            String newfilename = "";
            Pattern pat = null;
            if (!fileregex.equals("")) {
                pat = Pattern.compile(fileregex);
            }
            Matcher mat;
            for (int i = 0; i < fs.length; i++) {
                if (fs[i].isFile() && fs[i].lastModified() > maxtime) {
                    if (!fileregex.equals("")) {
                        mat = pat.matcher(fs[i].getName());
                        if (mat.find()) {
                            maxtime = fs[i].lastModified();
                            newfilename = fs[i].getAbsolutePath();
                        }
                    } else {
                        maxtime = fs[i].lastModified();
                        newfilename = fs[i].getAbsolutePath();
                    }


                }
            }
            return newfilename;
        }

        //写入文件名及行号
        public void writePosition(String path, long pos) {
            PropertyConfigurator.configure("conf/log4j.properties");
            Logger logger = Logger.getLogger(TailFileThread.class.getName());
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter("position/" + drname + ".position"));
                out.write(path + "," + pos);
                out.close();
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }

        public void run() {
            PropertyConfigurator.configure("conf/log4j.properties");
            Logger logger = Logger.getLogger(TailFileThread.class.getName());
            thisfile = getNewFile(file);
            try {
                BufferedReader br = new BufferedReader(new FileReader("position/" + drname + ".position"));
                String line = br.readLine();
                if (line != null) {
                    if (line.contains(",")) {
                        thisfile = line.split(",")[0];
                        beginln = Integer.parseInt(line.split(",")[1]);
                    }
                }


            } catch (FileNotFoundException e2) {
                // TODO Auto-generated catch block
                logger.error(e2.getMessage());

                //位置文件不存在，则创建
                try {
                    logger.info("create new file:" + "position/" + drname + ".position");
                    new File("position/" + drname + ".position").createNewFile();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            } catch (IOException e2) {
                logger.error(e2.getMessage());
            }

            //指定文件可读可写
            try {
                if (!thisfile.equals(""))
                    randomFile = new RandomAccessFile(thisfile, "rw");

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //位置文件记录的文件可能已经删除掉，获取最新的文件
                thisfile = getNewFile(file);
                try {
                    if (!thisfile.equals("")) {
                        randomFile = new RandomAccessFile(thisfile, "rw");
                        beginln = 0;
                    }
                } catch (FileNotFoundException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if (beginln > 0) {
                logger.info("seek to " + thisfile + " pos:" + beginln);
                try {
                    randomFile.seek(beginln);
                } catch (IOException e3) {
                    // TODO Auto-generated catch block
                    logger.error(e3.getMessage());
                }
            }

            while (true) {
                try {
                    Thread.sleep(1000);

                    //调用interrupt方法后
                    if (isInterrupted()) {
                        logger.info("Interrupted...");
                        break;
                    }
                } catch (InterruptedException e1) {
                    logger.error(e1.getMessage());
                }
                try {
                    //获得变化部分的
                    //  randomFile.seek(lastTimeFileSize);
                    String tmp = "";
                    String tmputf8 = "";

                    if (randomFile != null) {

                        while ((tmp = randomFile.readLine()) != null) {
                            tmputf8 = new String(tmp.getBytes("utf8"));
                            send(topicname, tmputf8);
                            ln++;
                            ln_s++;
                            //每发生一条写一次影响效率
                            if (ln > writepostion_rowcnt) {
                                writePosition(thisfile, randomFile.length());
                                ln = 0;
                            }
                            if (ln_s > pause_rouwcnt) {
                                ln_s = 0;
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }


                        }
                        prefile = thisfile;
                        thisfile = getNewFile(file);
                        if (!thisfile.equals(prefile) && !thisfile.equals("")) {
                            randomFile.close();
                            randomFile = new RandomAccessFile(thisfile, "rw");
                            //prefile=thisfile;
                            beginln = 0;
                        }
                    }
                    //while true循环发现randomFile为空
                    else {
                        thisfile = getNewFile(file);
                        if (!thisfile.equals("")) {
                            randomFile = new RandomAccessFile(thisfile, "rw");
                            beginln = 0;
                        }

                    }

                } catch (FileNotFoundException e1) {
                    logger.error(thisfile + ":" + e1.getMessage());
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    //正访问的文件被删除了,重新获取最新的文件
                    thisfile = getNewFile(file);
                    try {
                        if (!thisfile.equals("")) {
                            randomFile = new RandomAccessFile(thisfile, "rw");
                            beginln = 0;
                        }
                    } catch (FileNotFoundException e2) {
                        // TODO Auto-generated catch block
                        logger.error(e2.getMessage());
                    }

                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }


        }
    }

    public static void main(String[] args) throws Exception {
        /*
         * 传参：
         * 必需：
         * path 文件路径
         * topic
         * 可选 :
         * is_multidir 是否多目录
         * fileregex  文件名正则匹配
         *
         *
         */
        if (args.length < 1) {
            System.out.println("必须传入参数");
            System.exit(1);
        }
        HashMap<String, String> param_value = new HashMap<String, String>();
        for (String para : args) {
            if (!para.contains("=")) {
                System.out.println("传入参数格式:param_name=param_value");
                System.exit(1);
            }
            String[] s = para.split("=");
            param_value.put(s[0], s[1]);


        }
        if (!param_value.containsKey("path")) {
            System.out.println("必须传入参数:path");
            System.exit(1);
        }
        if (!param_value.containsKey("topic")) {
            System.out.println("必须传入参数:topic");
            System.exit(1);
        }

        PropertyConfigurator.configure("conf/log4j.properties");
        Logger logger = Logger.getLogger(TailFileToKafka.class.getName());
        String topicname = param_value.get("topic");
        String pathname = param_value.get("path");
        String fileregex = "";
        int writepostion_rowcnt = 10;
        int pause_rouwcnt = 10000;
        if (param_value.containsKey("fileregex"))
            fileregex = param_value.get("fileregex");

        if (param_value.containsKey("writepostion_rowcnt"))
            writepostion_rowcnt = Integer.parseInt(param_value.get("writepostion_rowcnt"));
        if (param_value.containsKey("pause_rouwcnt"))
            pause_rouwcnt = Integer.parseInt(param_value.get("pause_rouwcnt"));


        HashMap<String, TailFileThread> hm = new HashMap<String, TailFileThread>();
        File datafile = new File(pathname);
        File[] fs = datafile.listFiles();
        if (param_value.containsKey("is_multidir")) {
            if (param_value.get("is_multidir").equals("true")) {
                while (true) {
                    fs = datafile.listFiles();
                    for (int i = 0; i < fs.length; i++) {
                        if (fs[i].isDirectory()) {
                            String path = fs[i].getAbsolutePath();
                            //以drname作为position文件名
                            String drname = path.replaceAll("/", "_");
                            if (!fileregex.equals("")) {
                                drname = drname + "\"" + fileregex + "\"";
                            }
                            if (hm.containsKey(path)) {
                                if (!hm.get(path).isAlive()) {
                                    hm.get(path).interrupt();

                                    TailFileThread tt = new TailFileThread(path, drname, topicname, fileregex, writepostion_rowcnt, pause_rouwcnt);
                                    tt.start();
                                    hm.put(path, tt);
                                    logger.info("thread:" + tt.getName() + " start to tail " + path);
                                    System.out.println("thread:" + tt.getName() + " start to tail " + path);
                                }

                            }
                            //如果不存在，新建
                            else {
                                TailFileThread tt = new TailFileThread(path, drname, topicname, fileregex, writepostion_rowcnt, pause_rouwcnt);

                                tt.start();
                                hm.put(path, tt);
                                logger.info("thread:" + tt.getName() + " start to tail " + path);
                                System.out.println("thread:" + tt.getName() + " start to tail " + path);


                            }


                        }
                    }
                    Thread.sleep(100);
                }
            } else {
                String drname = pathname.replaceAll("/", "_");
                if (!fileregex.equals("")) {
                    drname = drname + "\"" + fileregex + "\"";
                }
                TailFileThread tt = new TailFileThread(pathname, drname, topicname, fileregex, writepostion_rowcnt, pause_rouwcnt);
                tt.start();
                logger.info("thread:" + tt.getName() + " start to tail " + pathname);
                System.out.println("thread:" + tt.getName() + " start to tail " + pathname);
            }


        } else {
            String drname = pathname.replaceAll("/", "_");
            if (!fileregex.equals("")) {
                drname = drname + "\"" + fileregex + "\"";
            }
            TailFileThread tt = new TailFileThread(pathname, drname, topicname, fileregex, writepostion_rowcnt, pause_rouwcnt);
            tt.start();
            logger.info("thread:" + tt.getName() + " start to tail " + pathname);
            System.out.println("thread:" + tt.getName() + " start to tail " + pathname);
        }


    }

}
