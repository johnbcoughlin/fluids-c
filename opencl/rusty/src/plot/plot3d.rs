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

pub trait Plotter3D {
    fn create(x_min: f64, x_max: f64,
              y_min: f64, y_max: f64,
              z_min: f64, z_max: f64) -> Self;

    fn header(&mut self);

    fn plot(&mut self, xs: &Vector<f64>, ys: &Vector<f64>, zs: &Vector<f64>);

    fn replot(&mut self);
}

pub struct GnuplotPlotter3D {
    gnuplot: Child,
    path: PathBuf,
}

impl GnuplotPlotter3D {
    fn begin_plotting(&mut self, x_min: f64, x_max: f64, y_min: f64, y_max: f64,
                      z_min: f64, z_max: f64) {
        let mut stdin = (&mut self.gnuplot.stdin).as_mut().expect("No stdin");
        writeln!(stdin, "set xrange [{}:{}]", x_min, x_max);
        writeln!(stdin, "set yrange [{}:{}]", y_min, y_max);
        writeln!(stdin, "set zrange [{}:{}]", z_min, z_max);
        writeln!(stdin, "set dgrid3d 30,30");
        writeln!(stdin, "set hidden3d");
        writeln!(
            stdin,
            "splot \"{}\" u 1:2:3 with lines",
            self.path.to_str().unwrap()
        );
    }

}

impl Plotter3D for GnuplotPlotter3D {
    fn create(x_min: f64, x_max: f64, y_min: f64, y_max: f64,
                  z_min: f64, z_max: f64) -> GnuplotPlotter3D {
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
        let mut result = GnuplotPlotter3D { gnuplot, path };
        result.begin_plotting(x_min, x_max, y_min, y_max, z_min, z_max);
        result
    }

    fn header(&mut self) {
        let mut file = OpenOptions::new()
            .truncate(true)
            .write(true)
            .open(&self.path)
            .expect("could not open file for header");
        writeln!(file, "#\tX\tY\tU").expect("error!");
        file.flush().expect("error flushing file");
    }

    fn plot(&mut self, xs: &Vector<f64>, ys: &Vector<f64>, zs: &Vector<f64>) {
        assert_eq!(xs.size(), ys.size());
        assert_eq!(xs.size(), zs.size());

        let mut file = OpenOptions::new()
            .append(true)
            .open(&self.path)
            .expect("could not open file for plotting");

        for ((x, y), z) in xs.iter().zip(ys.iter()).zip(zs.iter()) {
            writeln!(file, "{}\t{}\t{}", x, y, z).expect("error");
        }
        file.flush().expect("error flushing file");
    }

    fn replot(&mut self) {
        let mut stdin = (&mut self.gnuplot.stdin).as_mut().expect("No stdin");
        writeln!(stdin, "replot").expect("error");
        thread::sleep(Duration::from_millis(100));
    }
}


struct GliumPlotter3D {

}
