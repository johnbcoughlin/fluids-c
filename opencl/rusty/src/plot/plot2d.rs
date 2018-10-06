extern crate rulinalg;
extern crate tempfile;

use self::rulinalg::vector::Vector;
use self::tempfile::tempdir;
use std::fs::OpenOptions;
use std::io::Write;
use std::path::PathBuf;
use std::process::{Child, Command, Stdio};
use std::thread;
use std::time::Duration;

pub struct Plotter2D {
    gnuplot: Child,
    path: PathBuf,
}

impl Plotter2D {
    pub fn create(x_min: f64, x_max: f64, y_min: f64, y_max: f64) -> Plotter2D {
        let dir = tempdir()
            .expect("could not open temporary directory")
            .into_path();
        let path = dir.join("data");
        println!("Data file: {}", path.to_str().unwrap());
        let file = OpenOptions::new()
            .create(true)
            .write(true)
            .open(&path)
            .expect("could not create data file");
        let mut gnuplot = Command::new("gnuplot")
            .arg("-p")
            .stdin(Stdio::piped())
            .spawn()
            .ok()
            .expect("Couldn't spawn gnuplot. Make sure it's installed and on the PATH");
        let mut result = Plotter2D { gnuplot, path };
        result.begin_plotting(x_min, x_max, y_min, y_max);
        result
    }

    fn begin_plotting(&mut self, x_min: f64, x_max: f64, y_min: f64, y_max: f64) {
        let mut stdin = (&mut self.gnuplot.stdin).as_mut().expect("No stdin");
        writeln!(stdin, "set xrange [{}:{}]", x_min, x_max);
        writeln!(stdin, "set yrange [{}:{}]", y_min, y_max);
        writeln!(
            stdin,
            "plot \"{}\" using 1:2 with lines",
            self.path.to_str().unwrap()
        );
    }

    pub fn header(&mut self) {
        let mut file = OpenOptions::new()
            .truncate(true)
            .write(true)
            .open(&self.path)
            .expect("could not open file for header");
        writeln!(file, "#\tX\tU").expect("error!");
        file.flush().expect("error flushing file");
    }

    pub fn plot(&mut self, xs: &Vector<f64>, ys: &Vector<f64>) {
        assert_eq!(xs.size(), ys.size());

        let mut file = OpenOptions::new()
            .append(true)
            .open(&self.path)
            .expect("could not open file for plotting");

        for (x, y) in xs.iter().zip(ys.iter()) {
            writeln!(file, "{}\t{}", x, y).expect("error");
        }
        file.flush().expect("error flushing file");
    }

    pub fn replot(&mut self) {
        let mut stdin = (&mut self.gnuplot.stdin).as_mut().expect("No stdin");
        writeln!(stdin, "replot").expect("error");
        thread::sleep(Duration::from_millis(100));
    }
}

impl Drop for Plotter2D {
    fn drop(&mut self) {
        self.gnuplot.kill().expect("error killing gnuplot");
    }
}

